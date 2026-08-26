package com.syncturtle.platform.gateway.security.session;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.cache.template.RedisKeyBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class PassportRedisKeys {

    private static final String OWNER = "user-service";
    private static final String RESOURCE_REFRESH_SESSION = "refresh-session";
    private static final String RESOURCE_USER_AUTH_VERSION = "user-auth-version";
    private static final String RESOURCE_ADMIN_SESSION_VERSION = "admin-session-version";
    private static final String SCOPE_SESSION_ID = "sid";
    private static final String SCOPE_USER_ID = "u";
    private static final String SCOPE_INSTANCE_ID = "i";

    private final RedisKeyBuilder redisKeyBuilder;

    public String sessionKey(String sessionId) {
        Assert.hasText(sessionId, "sessionId is required");

        return redisKeyBuilder.authKey(OWNER, RESOURCE_REFRESH_SESSION, SCOPE_SESSION_ID, sessionId);
    }

    public String userAuthVersionKey(String userId) {
        Assert.hasText(userId, "userId is required");

        return redisKeyBuilder.authKey(OWNER, RESOURCE_USER_AUTH_VERSION, SCOPE_USER_ID, userId);
    }

    public String adminSessionVersionKey(String instanceId, String userId) {
        Assert.hasText(instanceId, "instanceId is required");
        Assert.hasText(userId, "userId is required");

        return redisKeyBuilder.authKey(OWNER, RESOURCE_ADMIN_SESSION_VERSION, SCOPE_INSTANCE_ID, instanceId,
                SCOPE_USER_ID, userId);
    }

}
