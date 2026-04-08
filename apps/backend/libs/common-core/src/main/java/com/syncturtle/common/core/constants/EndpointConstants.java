package com.syncturtle.common.core.constants;

public final class EndpointConstants {
    public static final String AUTH = "/auth";
    public static final String EMAIL__CHECK = "/email-check";
    public static final String SIGN__OUT = "/sign-out";

    // internal endpoints
    public static final String INTERNAL_V1 = "/internal/v1";
    // user-service
    public static final String INTERNAL_V1_USERS = INTERNAL_V1 + "/users";
    public static final String ADMINS_SIGN__UP = "/admins/sign-up";
    public static final String ADMINS_SIGN__IN = "/admins/sign-in";
    // instance-service
    public static final String API_INSTANCES = "/api/instances";
    public static final String CONFIGURATIONS = "/configurations";
    public static final String CONFIGURATIONS_DISABLE_EMAIL_FEATURE = CONFIGURATIONS + "/disable-email-feature";
    public static final String ADMINS = "/admins";
    public static final String ADMINS_ME = ADMINS + "/me";

    private EndpointConstants() {
    }
}
