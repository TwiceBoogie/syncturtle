package com.syncturtle.common.contracts.workspace.type;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkspaceRole {

    GUEST(WorkspaceRoleCodes.GUEST),
    MEMBER(WorkspaceRoleCodes.MEMBER),
    ADMIN(WorkspaceRoleCodes.ADMIN);

    private final int code;

    private static final Map<Integer, WorkspaceRole> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(WorkspaceRole::getCode, role -> role));

    WorkspaceRole(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    public boolean isAtLeast(WorkspaceRole requiredRole) {
        if (requiredRole == null) {
            throw new IllegalArgumentException("requiredRole is required");
        }

        return this.code >= requiredRole.code;
    }

    public boolean isHigherThan(WorkspaceRole otherRole) {
        if (otherRole == null) {
            throw new IllegalArgumentException("otherRole is required");
        }

        return this.code > otherRole.code;
    }

    public boolean canInvite(WorkspaceRole invitedRole) {
        if (invitedRole == null) {
            throw new IllegalArgumentException("invitedRole is required");
        }

        return isAtLeast(invitedRole);
    }

    public boolean canManageWorkspace() {
        return this == ADMIN;
    }

    public boolean canManageMembers() {
        return this == ADMIN;
    }

    public static Optional<WorkspaceRole> fromCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }

    @JsonCreator
    public static WorkspaceRole requireFromCode(int code) {
        return fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workspace role code: " + code));
    }

}