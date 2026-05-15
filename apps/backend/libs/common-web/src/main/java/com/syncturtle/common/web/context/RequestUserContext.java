package com.syncturtle.common.web.context;

import java.util.UUID;

public final class RequestUserContext {

    private final ThreadLocal<State> state = ThreadLocal.withInitial(State::new);

    public UUID getUserId() {
        return state.get().userId;
    }

    public void setUserId(UUID userId) {
        state.get().userId = userId;
    }

    public boolean isAuthenticated() {
        return getUserId() != null;
    }

    public void clear() {
        state.remove();
    }

    private static final class State {
        private UUID userId;
    }
}
