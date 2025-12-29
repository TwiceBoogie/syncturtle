package com.syncturtle.common.web.context;

import java.util.UUID;

public final class RequestUserContext {

    private final ThreadLocal<UUID> userId = new ThreadLocal<>();

    public void setUserId(UUID userId) {
        this.userId.set(userId);
    }

    public UUID getUserId() {
        return userId.get();
    }

    public void clear() {
        userId.remove();
    }
}
