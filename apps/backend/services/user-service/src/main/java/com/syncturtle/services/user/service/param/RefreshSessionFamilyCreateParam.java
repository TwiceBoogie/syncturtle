package com.syncturtle.services.user.service.param;

import java.util.List;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class RefreshSessionFamilyCreateParam {

    private final String userId;
    private final String instanceId;
    private final List<String> roles;
    private final long authVersion;
    private final Long adminSessionVersion;
    private final String deviceLabel;
    private final String clientBindingHash;

    @Builder
    private RefreshSessionFamilyCreateParam(
            String userId,
            String instanceId,
            List<String> roles,
            Long authVersion,
            Long adminSessionVersion,
            String deviceLabel,
            String clientBindingHash) {
        Assert.hasText(userId, "userId is required");
        Assert.hasText(instanceId, "instanceId is required");
        Assert.notEmpty(roles, "roles are required");
        Assert.notNull(authVersion, "authVersion is required");
        Assert.isTrue(authVersion >= 0, "authVersion must not be negative");
        Assert.hasText(clientBindingHash, "clientBindingHash is required");

        this.userId = userId;
        this.instanceId = instanceId;
        this.roles = List.copyOf(roles);
        this.authVersion = authVersion;
        this.adminSessionVersion = adminSessionVersion;
        this.deviceLabel = deviceLabel;
        this.clientBindingHash = clientBindingHash;
    }

}
