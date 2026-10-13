package com.syncturtle.platform.gateway.type;

public enum GatewayCsrfRequirement {
    NONE,
    PREAUTH,
    AUTHENTICATED_SESSION,
    TRANSPORT_SESSION
}
