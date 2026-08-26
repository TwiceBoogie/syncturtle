package com.syncturtle.platform.gateway.filter;

import org.springframework.core.Ordered;

public final class GatewayFilterOrders {

    private GatewayFilterOrders() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final int STRIP_INBOUND_AUTH_HEADERS = Ordered.HIGHEST_PRECEDENCE;
    public static final int CLIENT_METADATA_HEADERS = Ordered.HIGHEST_PRECEDENCE + 10;
    public static final int AUTHENTICATED_PASSPORT_HEADERS = Ordered.HIGHEST_PRECEDENCE + 20;
    public static final int CSRF = Ordered.HIGHEST_PRECEDENCE + 30;
    public static final int BROWSER_SECURITY_COOKIE_ISOLATION = Ordered.HIGHEST_PRECEDENCE + 40;
    /**
     * Runs after authentication/header-enrichment filters but before spring cloud
     * gateway's final routing filter
     */
    public static final int ACCESS_LOG = Ordered.LOWEST_PRECEDENCE - 100;

}
