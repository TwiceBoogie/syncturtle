package com.syncturtle.services.workspace.type;

public enum OutboxStatus {
    NEW,
    PUBLISHING,
    PUBLISHED,
    FAILED,
    DEAD
}
