package com.syncturtle.services.user.service.session;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SessionVersionWriter {

    private final StringRedisTemplate redis;
    private final RefreshSessionTokenStore refreshSessionTokenStore;

    public void write(
            String userId,
            String instanceId,
            Long userAuthVersion,
            Long adminSessionVersion) {
        Assert.hasText(userId, "userId is required");
        Assert.hasText(instanceId, "instanceId is required");
        Assert.notNull(userAuthVersion, "userAuthVersion is required");

        redis.opsForValue().set(
                refreshSessionTokenStore.currentUserAuthVersionKey(userId),
                Long.toString(userAuthVersion));

        if (adminSessionVersion != null) {
            redis.opsForValue().set(
                    refreshSessionTokenStore.currentAdminSessionVersionKey(instanceId, userId),
                    Long.toString(adminSessionVersion));
        }
    }

}
