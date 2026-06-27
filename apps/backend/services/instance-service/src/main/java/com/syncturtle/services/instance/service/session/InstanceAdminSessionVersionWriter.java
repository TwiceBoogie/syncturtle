package com.syncturtle.services.instance.service.session;

import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.cache.template.RedisKeyBuilder;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstanceAdminSessionVersionWriter {

    private static final String OWNER = "user-service";
    private static final String RESOURCE_ADMIN_SESSION_VERSION = "admin-session-version";
    private static final String SCOPE_INSTANCE_ID = "i";
    private static final String SCOPE_USER_ID = "u";

    private final StringRedisTemplate redis;
    private final RedisKeyBuilder redisKeyBuilder;

    public void write(UUID instanceId, UUID userId, Long sessionVersion) {
        Assert.notNull(instanceId, "instanceId is required");
        Assert.notNull(userId, "userId is required");
        Assert.notNull(sessionVersion, "sessionVersion is required");

        redis.opsForValue().set(key(instanceId, userId), String.valueOf(sessionVersion));
    }

    public void delete(UUID instanceId, UUID userId) {
        Assert.notNull(instanceId, "instanceId is required");
        Assert.notNull(userId, "userId is required");

        redis.delete(key(instanceId, userId));
    }

    private String key(UUID instanceId, UUID userId) {
        return redisKeyBuilder.authKey(
                OWNER,
                RESOURCE_ADMIN_SESSION_VERSION,
                SCOPE_INSTANCE_ID,
                instanceId.toString(),
                SCOPE_USER_ID,
                userId.toString());
    }

}
