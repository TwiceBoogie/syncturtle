package com.syncturtle.platform.gateway.type;

public enum GatewayCsrfRequirement {
    NONE,
    PREAUTH,
    PREAUTH_COOKIE,
    AUTHENTICATED_SESSION,
    TRANSPORT_SESSION
}
