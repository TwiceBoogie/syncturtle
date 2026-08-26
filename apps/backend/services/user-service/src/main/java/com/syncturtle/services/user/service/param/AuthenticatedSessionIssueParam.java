package com.syncturtle.services.user.service.param;

import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.services.user.model.User;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthenticatedSessionIssueParam {

    User user;
    UUID instanceId;
    String ipAddress;
    String userAgent;

    private AuthenticatedSessionIssueParam(
            User user,
            UUID instanceId,
            String ipAddress,
            String userAgent) {
        Assert.notNull(user, "user is required");
        Assert.notNull(instanceId, "instanceId is required");

        this.user = user;
        this.instanceId = instanceId;
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
