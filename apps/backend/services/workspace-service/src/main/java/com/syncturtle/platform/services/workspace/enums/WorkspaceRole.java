package com.syncturtle.platform.services.workspace.enums;

public enum WorkspaceRole {
    ADMIN(20),
    MEMBER(15),
    GUEST(5);

    public final int code;

    WorkspaceRole(int code) {
        this.code = code;
    }

    public static WorkspaceRole from(int code) {
        for (WorkspaceRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        throw new IllegalArgumentException("Uknown role code: " + code);
    }
}
