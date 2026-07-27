package com.syncturtle.services.instance.type;

public enum OutboxStatus {
    NEW,
    PUBLISHING,
    PUBLISHED,
    FAILED,
    DEAD
}
