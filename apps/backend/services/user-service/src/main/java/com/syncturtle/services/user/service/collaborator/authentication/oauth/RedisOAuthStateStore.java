package com.syncturtle.services.user.service.collaborator.authentication.oauth;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.cache.template.RedisKeyBuilder;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.provider.AuthProvider;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class RedisOAuthStateStore implements OAuthStateStore {

    private static final Duration EXPIRATION = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final JsonMapper jsonMapper;
    private final RedisKeyBuilder keyBuilder;

    @Override
    public void save(OAuthState state, AuthProvider provider) {
        Assert.notNull(state, provider.name().toLowerCase() + " oauth state is required");
        Assert.hasText(state.getState(), provider.name().toLowerCase() + " oauth state value is required");

        try {
            String json = jsonMapper.writeValueAsString(state);
            redis.opsForValue().set(redisKey(state.getState(), provider), json, EXPIRATION);
        } catch (Exception exception) {
            throw AuthException.of(AuthErrorCode.GOOGLE_OAUTH_PROVIDER_ERROR);
        }
    }

    @Override
    public Optional<OAuthState> consume(String state, AuthProvider provider) {
        Assert.hasText(state, provider.name().toLowerCase() + " oauth state value is required");

        String redisKey = redisKey(state, provider);
        String json = redis.opsForValue().getAndDelete(redisKey);

        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }

        try {
            return Optional.of(jsonMapper.readValue(json, OAuthState.class));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String redisKey(String state, AuthProvider provider) {
        String encodedState = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(state.getBytes(StandardCharsets.UTF_8));
        return keyBuilder.authKey("user-service", "oauth-state", provider.name(), encodedState);
    }

}
