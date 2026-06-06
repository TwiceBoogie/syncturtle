package com.syncturtle.common.web.context;

import java.util.UUID;

public final class RequestUserContext {

    private final ThreadLocal<RequestUser> currentUser = ThreadLocal.withInitial(RequestUser::anonymous);

    public RequestUser getCurrentUser() {
        return currentUser.get();
    }

    public UUID getUserId() {
        return getCurrentUser().getUserId();
    }

    public UUID requireUserId() {
        return getCurrentUser().requireUserId();
    }

    public boolean isAuthenticated() {
        return getCurrentUser().isAuthenticated();
    }

    public void setAuthenticated(UUID userId) {
        currentUser.set(RequestUser.authenticated(userId));
    }

    public void setAnonymous() {
        currentUser.set(RequestUser.anonymous());
    }

    public void clear() {
        currentUser.remove();
    }

}
