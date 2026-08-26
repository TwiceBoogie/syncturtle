package com.syncturtle.services.user.service.collaborator.session;

import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.cache.template.RedisKeyBuilder;

public final class AdminSessionHandoffRedisKeys {

    private static final String OWNER = "user-service";
    private static final String RESOURCE = "admin-session-handoff";
    private static final String RECEIPT = "receipt";

    private final RedisKeyBuilder keyBuilder;

    public AdminSessionHandoffRedisKeys(RedisKeyBuilder keyBuilder) {
        Assert.notNull(keyBuilder, "redis key builder is required");

        this.keyBuilder = keyBuilder;
    }

    public String handoff(String receiptId) {
        Assert.hasText(receiptId, "receiptId is required");

        String normalized = receiptId.trim();
        String canonical = UUID.fromString(normalized).toString();
        Assert.isTrue(canonical.equals(normalized), "receiptId must be a canonical UUID");
        return keyBuilder.authKey(OWNER, RESOURCE, RECEIPT, canonical);
    }

}
