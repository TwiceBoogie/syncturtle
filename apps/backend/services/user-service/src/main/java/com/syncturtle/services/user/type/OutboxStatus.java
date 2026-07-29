package com.syncturtle.services.user.type;

public enum OutboxStatus {
    NEW,
    PUBLISHING,
    PUBLISHED,
    FAILED,
    DEAD
}
