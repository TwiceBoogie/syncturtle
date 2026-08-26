package com.syncturtle.services.user.service.param;

import java.util.List;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class AuthorizedSessionIssueParam {

    private final String userId;
    private final String instanceId;
    private final List<String> roles;
    private final long authVersion;
    private final Long adminSessionVersion;
    private final String ipAddress;
    private final String userAgent;

    @Builder
    private AuthorizedSessionIssueParam(
            String userId,
            String instanceId,
            List<String> roles,
            Long authVersion,
            Long adminSessionVersion,
            String ipAddress,
            String userAgent) {
        Assert.hasText(userId, "userId is required");
        Assert.hasText(instanceId, "instanceId is required");
        Assert.notEmpty(roles, "roles are required");
        Assert.notNull(authVersion, "authVersion is required");
        Assert.isTrue(authVersion >= 0, "authVersion must not be negative");

        this.userId = userId;
        this.instanceId = instanceId;
        this.roles = List.copyOf(roles);
        this.authVersion = authVersion;
        this.adminSessionVersion = adminSessionVersion;
        this.ipAddress = normalizeNullable(ipAddress);
        this.userAgent = normalizeNullable(userAgent);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
