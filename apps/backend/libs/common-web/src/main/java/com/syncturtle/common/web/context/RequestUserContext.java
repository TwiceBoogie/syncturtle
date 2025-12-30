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

    public boolean isAuthenticated() {
        return getUserId() != null;
    }

    public void clear() {
        userId.remove();
    }
}
