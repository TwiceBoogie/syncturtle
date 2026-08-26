package com.syncturtle.services.user.service.param;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class AdminSessionHandoffCreateParam {

    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    private final UUID userId;
    private final UUID instanceId;
    private final long userAuthVersion;
    private final long adminSessionVersion;
    private final String preAuthBindingHash;
    private final String clientBindingHash;

    @Builder
    private AdminSessionHandoffCreateParam(
            UUID userId,
            UUID instanceId,
            long userAuthVersion,
            long adminSessionVersion,
            String preAuthBindingHash,
            String clientBindingHash) {
        Assert.notNull(userId, "userId is required");
        Assert.notNull(instanceId, "instanceId is required");
        Assert.isTrue(userAuthVersion >= 0, "userAuthVersion must not be negative");
        Assert.isTrue(adminSessionVersion >= 0, "adminSessionVersion must not be negative");

        verifyHashPattern(preAuthBindingHash, "preAuthBindingHash must be a lowercase SHA-256 value");
        verifyHashPattern(clientBindingHash, "clientBindingHash must be a lowercase SHA-256 value");

        this.userId = userId;
        this.instanceId = instanceId;
        this.userAuthVersion = userAuthVersion;
        this.adminSessionVersion = adminSessionVersion;
        this.preAuthBindingHash = preAuthBindingHash;
        this.clientBindingHash = clientBindingHash;
    }

    private static void verifyHashPattern(String hash, String message) {
        Assert.isTrue(hash != null && HASH.matcher(hash).matches(), message);
    }

}
