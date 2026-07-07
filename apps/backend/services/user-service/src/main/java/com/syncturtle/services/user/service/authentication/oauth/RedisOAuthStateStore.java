package com.syncturtle.services.user.service.authentication.oauth;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.provider.AuthProvider;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisOAuthStateStore implements OAuthStateStore {

    private static final Duration EXPIRATION = Duration.ofMinutes(10);
    private static final String GOOGLE_KEY_PREFIX = "oauth_google_state_";
    private static final String GITLAB_KEY_PREFIX = "oauth_gitlab_state_";
    private static final String GITHUB_KEY_PREFIX = "oauth_github_state_";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public void save(OAuthState state, AuthProvider provider) {
        Assert.notNull(state, provider.name().toLowerCase() + " oauth state is required");
        Assert.hasText(state.getState(), provider.name().toLowerCase() + " oauth state value is required");

        try {
            String json = objectMapper.writeValueAsString(state);
            redis.opsForValue().set(redisKey(state.getState(), provider), json, EXPIRATION);
        } catch (Exception exception) {
            throw AuthException.of(AuthErrorCode.GOOGLE_OAUTH_PROVIDER_ERROR);
        }
    }

    @Override
    public Optional<OAuthState> consume(String state, AuthProvider provider) {
        Assert.hasText(state, provider.name().toLowerCase() + " oauth state value is required");

        String redisKey = redisKey(state, provider);
        String json = redis.opsForValue().get(redisKey);

        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }

        redis.delete(redisKey);

        try {
            return Optional.of(objectMapper.readValue(json, OAuthState.class));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String redisKey(String state, AuthProvider provider) {
        switch (provider) {
            case GOOGLE:
                return GOOGLE_KEY_PREFIX + state;
            case GITHUB:
                return GITHUB_KEY_PREFIX + state;
            case GITLAB:
                return GITLAB_KEY_PREFIX + state;
            default:
                throw new IllegalArgumentException("Uknown auth provider");
        }
    }

}
