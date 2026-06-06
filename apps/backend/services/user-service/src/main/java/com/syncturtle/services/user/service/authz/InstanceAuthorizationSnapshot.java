package com.syncturtle.services.user.service.authz;

import java.util.List;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class InstanceAuthorizationSnapshot {

    private static final String USER_ROLE = "USER";
    private static final String INSTANCE_ADMIN_ROLE = "INSTANCE_ADMIN";

    private final List<String> roles;
    private final Long adminSessionVersion;

    private InstanceAuthorizationSnapshot(List<String> roles, Long adminSessionVersion) {
        Assert.notNull(roles, "roles is required");
        Assert.isTrue(!roles.isEmpty(), "roles must not be empty");

        this.roles = List.copyOf(roles);
        this.adminSessionVersion = adminSessionVersion;
    }

    public static InstanceAuthorizationSnapshot instanceAdmin(Long adminSessionVersion) {
        Assert.notNull(adminSessionVersion, "adminSessionVersion is required");

        return new InstanceAuthorizationSnapshot(List.of(USER_ROLE, INSTANCE_ADMIN_ROLE), adminSessionVersion);
    }

    public static InstanceAuthorizationSnapshot member() {
        return new InstanceAuthorizationSnapshot(List.of(USER_ROLE), null);
    }

    public boolean isInstanceAdmin() {
        return roles.contains(INSTANCE_ADMIN_ROLE);
    }

}
