package com.syncturtle.common.web.context;

public final class RequestClientContext {

    private final ThreadLocal<State> state = ThreadLocal.withInitial(State::new);

    public String getClientIp() {
        return state.get().clientIp;
    }

    public String getUserAgent() {
        return state.get().userAgent;
    }

    public void setClientIp(String ip) {
        state.get().clientIp = ip;
    }

    public void setUserAgent(String ua) {
        state.get().userAgent = ua;
    }

    public void clear() {
        state.remove();
    }

    private static final class State {
        private String clientIp;
        private String userAgent;
    }

}
