package com.syncturtle.platform.tests.env;

public final class ServiceUrls {

    private ServiceUrls() {
    }

    public static final String GATEWAY_BASE_URL = "syncturtle.gateway.base-url";

    public static String gateway() {
        String url = System.getProperty(GATEWAY_BASE_URL);
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Missing system property: " + GATEWAY_BASE_URL
                    + " (SyncturtleEnvironment should set it in beforeAll)");
        }
        return url;
    }

}
