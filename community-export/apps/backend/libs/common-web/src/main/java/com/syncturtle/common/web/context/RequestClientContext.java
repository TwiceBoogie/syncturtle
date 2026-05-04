package com.syncturtle.common.web.context;

public final class RequestClientContext {

    private static final ThreadLocal<State> TL = ThreadLocal.withInitial(State::new);

    public String getClientIp() {
        return TL.get().clientIp;
    }

    public String getUserAgent() {
        return TL.get().userAgent;
    }

    public void setClientIp(String ip) {
        TL.get().clientIp = ip;
    }

    public void setUserAgent(String ua) {
        TL.get().userAgent = ua;
    }

    public void clear() {
        TL.remove();
    }

    private static final class State {
        private String clientIp;
        private String userAgent;
    }

}
