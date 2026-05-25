package com.syncturtle.common.web.context;

import java.util.Objects;
import java.util.UUID;

public final class RequestUser {

    private static final RequestUser ANONYMOUS = new RequestUser(null);

    private final UUID userId;

    private RequestUser(UUID userId) {
        this.userId = userId;
    }

    public static RequestUser anonymous() {
        return ANONYMOUS;
    }

    public static RequestUser authenticated(UUID userId) {
        return new RequestUser(Objects.requireNonNull(userId, "userId is required"));
    }

    public boolean isAuthenticated() {
        return userId != null;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID requireUserId() {
        if (userId == null) {
            throw new IllegalStateException("Authenticated user is required.");
        }
        return userId;
    }

}
