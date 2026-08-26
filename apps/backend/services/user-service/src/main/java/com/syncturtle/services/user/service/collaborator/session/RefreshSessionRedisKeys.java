package com.syncturtle.services.user.service.collaborator.session;

import org.springframework.util.Assert;

import com.syncturtle.common.cache.template.RedisKeyBuilder;

public final class RefreshSessionRedisKeys {

    private static final String OWNER = "user-service";
    private static final String SESSION_RESOURCE = "refresh-session";
    private static final String GRACE_RESOURCE = "refresh-grace";
    private static final String BASE_INDEX_RESOURCE = "refresh-sessions-by-user";
    private static final String ELEVATED_INDEX_RESOURCE = "elevated-refresh-sessions-by-user";
    private static final String USER_AUTH_VERSION_RESOURCE = "user-auth-version";
    private static final String ADMIN_SESSION_VERSION_RESOURCE = "admin-session-version";
    private static final String SID = "sid";
    private static final String USER = "u";
    private static final String INSTANCE = "i";

    private final RedisKeyBuilder keyBuilder;

    public RefreshSessionRedisKeys(RedisKeyBuilder keyBuilder) {
        Assert.notNull(keyBuilder, "redis key builder is required");

        this.keyBuilder = keyBuilder;
    }

    public String session(String sessionId) {
        Assert.hasText(sessionId, "sessionId is required");

        return keyBuilder.authKey(OWNER, SESSION_RESOURCE, SID, sessionId);
    }

    public String grace(String sessionId) {
        Assert.hasText(sessionId, "sessionId is required");

        return keyBuilder.authKey(OWNER, GRACE_RESOURCE, SID, sessionId);
    }

    public String baseIndex(String userId) {
        Assert.hasText(userId, "userId is required");

        return keyBuilder.authKey(OWNER, BASE_INDEX_RESOURCE, USER, userId);
    }

    public String elevatedIndex(String userId) {
        Assert.hasText(userId, "userId is required");

        return keyBuilder.authKey(OWNER, ELEVATED_INDEX_RESOURCE, USER, userId);
    }

    public String userAuthVersion(String userId) {
        Assert.hasText(userId, "userId is required");

        return keyBuilder.authKey(OWNER, USER_AUTH_VERSION_RESOURCE, USER, userId);
    }

    public String adminSessionVersion(String instanceId, String userId) {
        Assert.hasText(instanceId, "instanceId is required");
        Assert.hasText(userId, "userId is required");

        return keyBuilder.authKey(OWNER, ADMIN_SESSION_VERSION_RESOURCE, INSTANCE, instanceId, USER, userId);
    }

    public String sessionPrefix() {
        return keyBuilder.authKey(OWNER, SESSION_RESOURCE, SID) + ":";
    }

    public String gracePrefix() {
        return keyBuilder.authKey(OWNER, GRACE_RESOURCE, SID) + ":";
    }

}
