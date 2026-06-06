package com.syncturtle.services.instance.type;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum InstanceAdminRole {
    GUEST(InstanceAdminRoleCodes.GUEST),
    USER(InstanceAdminRoleCodes.USER),
    ADMIN(InstanceAdminRoleCodes.ADMIN);

    private final int code;

    // Thread-safe O(1) constant-time lookup cache
    private static final Map<Integer, InstanceAdminRole> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(InstanceAdminRole::getCode, role -> role));

    InstanceAdminRole(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean isAtLeast(InstanceAdminRole requiredRole) {
        if (requiredRole == null) {
            throw new IllegalArgumentException("requiredRole is required");
        }

        return this.code >= requiredRole.code;
    }

    public static Optional<InstanceAdminRole> fromCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }
}
