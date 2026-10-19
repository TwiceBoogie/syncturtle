package com.syncturtle.services.user.service.collaborator.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.syncturtle.common.cache.property.RedisKeyProperties;
import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.provider.AuthProvider;
import com.syncturtle.services.user.service.collaborator.authentication.magic.MagicCodeChallenge;
import com.syncturtle.services.user.service.collaborator.authentication.magic.MagicCodeStore;
import com.syncturtle.services.user.service.collaborator.authentication.magic.RedisMagicCodeStore;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.OAuthState;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.RedisOAuthStateStore;

import tools.jackson.databind.json.JsonMapper;

class RedisAuthenticationNamespaceTest {

    private final Map<String, String> contents = new HashMap<>();
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final JsonMapper jsonMapper = mock(JsonMapper.class);
    private final RedisKeyBuilder localKeys = new RedisKeyBuilder(new RedisKeyProperties("st:local:"));
    private final RedisKeyBuilder otherKeys = new RedisKeyBuilder(new RedisKeyProperties("st:other:"));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void configureRedisStorage() {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(invocation -> contents.get(invocation.getArgument(0)));
        when(values.getAndDelete(anyString())).thenAnswer(invocation -> contents.remove(invocation.getArgument(0)));
        doAnswer(invocation -> {
            contents.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(values).set(anyString(), anyString(), any(Duration.class));
        when(redis.delete(anyString())).thenAnswer(invocation -> contents.remove(invocation.getArgument(0)) != null);
    }

    @Nested
    class MagicCodes {

        @Test
        void creationAndVerificationShareTheRootAndNormalizedEmail() {
            RedisMagicCodeStore store = new RedisMagicCodeStore(redis, jsonMapper, localKeys);
            when(jsonMapper.writeValueAsString(any())).thenReturn("magic-payload");
            MagicCodeChallenge challenge = store.createOrRotate(" User@Example.com ",
                    AuthErrorCode.EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_IN);
            RedisMagicCodeStore.MagicCodePayload payload = new RedisMagicCodeStore.MagicCodePayload();
            payload.setToken(challenge.getToken());
            when(jsonMapper.readValue("magic-payload", RedisMagicCodeStore.MagicCodePayload.class)).thenReturn(payload);

            MagicCodeStore.VerificationResult result = store.verify("user@example.com", challenge.getToken());

            assertThat(challenge.getKey()).startsWith("st:local:auth:user-service:magic-code:email:");
            assertThat(result.isVerified()).isTrue();
            assertThat(contents).isEmpty();
        }

        @Test
        void anotherInstallationCannotVerifyOrDeleteTheChallenge() {
            RedisMagicCodeStore local = new RedisMagicCodeStore(redis, jsonMapper, localKeys);
            RedisMagicCodeStore other = new RedisMagicCodeStore(redis, jsonMapper, otherKeys);
            when(jsonMapper.writeValueAsString(any())).thenReturn("magic-payload");
            MagicCodeChallenge challenge = local.createOrRotate("user@example.com",
                    AuthErrorCode.EMAIL_CODE_ATTEMPT_EXHAUSTED_SIGN_IN);

            MagicCodeStore.VerificationResult result = other.verify("user@example.com", challenge.getToken());

            assertThat(result.getFailureReason()).isEqualTo(MagicCodeStore.FailureReason.EXPIRED);
            assertThat(contents).containsKey(challenge.getKey());
        }
    }

    @Nested
    class OAuthStates {

        @ParameterizedTest
        @EnumSource(AuthProvider.class)
        void saveAndConsumeUseTheInstallationRootForEveryProvider(AuthProvider provider) {
            RedisOAuthStateStore store = new RedisOAuthStateStore(redis, jsonMapper, localKeys);
            OAuthState state = OAuthState.builder().state("opaque:state").nextPath("/")
                    .browserBindingHash("binding-hash").build();
            when(jsonMapper.writeValueAsString(state)).thenReturn("oauth-payload");
            when(jsonMapper.readValue("oauth-payload", OAuthState.class)).thenReturn(state);
            store.save(state, provider);
            assertThat(contents).hasSize(1);
            assertThat(contents.keySet()).allMatch(key -> key.startsWith("st:local:auth:user-service:oauth-state:"));

            var consumed = store.consume(state.getState(), provider);

            assertThat(consumed).contains(state);
            assertThat(store.consume(state.getState(), provider)).isEmpty();
            assertThat(contents).isEmpty();
        }

        @Test
        void anotherInstallationCannotConsumeOrDeleteOAuthState() {
            RedisOAuthStateStore local = new RedisOAuthStateStore(redis, jsonMapper, localKeys);
            RedisOAuthStateStore other = new RedisOAuthStateStore(redis, jsonMapper, otherKeys);
            OAuthState state = OAuthState.builder().state("opaque-state").nextPath("/").build();
            when(jsonMapper.writeValueAsString(state)).thenReturn("oauth-payload");
            local.save(state, AuthProvider.GOOGLE);

            var consumed = other.consume(state.getState(), AuthProvider.GOOGLE);

            assertThat(consumed).isEmpty();
            assertThat(contents).hasSize(1);
        }
    }

}
