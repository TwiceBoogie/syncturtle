package com.syncturtle.common.core.actor;

public enum PrincipalType {
    HUMAN,
    BOT,
    SERVICE_ACCOUNT,
    SYSTEM;

    public boolean isHuman() {
        return this == HUMAN;
    }

    public boolean canLoginInteractively() {
        return this == HUMAN;
    }

    public boolean isMachine() {
        return this == SYSTEM || this == BOT || this == SERVICE_ACCOUNT;
    }
}
