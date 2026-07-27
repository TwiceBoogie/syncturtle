package com.syncturtle.platform.gateway.filters;

public final class GatewayExchangeAttributes {

    private GatewayExchangeAttributes() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String REQUEST_START_NANOS = GatewayExchangeAttributes.class.getName() + ".requestStartNanos";
    public static final String FAILURE = GatewayExchangeAttributes.class.getName() + ".failure";

}
