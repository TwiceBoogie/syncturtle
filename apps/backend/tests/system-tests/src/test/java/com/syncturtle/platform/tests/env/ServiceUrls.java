package com.syncturtle.platform.tests.env;

public final class ServiceUrls {

    public static final String GATEWAY_BASE_URL = "syncturtle.gateway.base-url";

    private ServiceUrls() {
        throw new AssertionError("ServiceUrls must not be instantiated");
    }

    public static String gateway() {
        String url = System.getProperty(GATEWAY_BASE_URL);

        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Missing system property: " + GATEWAY_BASE_URL
                    + ". SyncturtleEnvironment must initialize " + "the system-test environment first.");
        }
        return url;
    }

}
