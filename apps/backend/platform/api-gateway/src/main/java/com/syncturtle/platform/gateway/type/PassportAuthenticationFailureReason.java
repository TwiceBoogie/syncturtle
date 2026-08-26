package com.syncturtle.platform.gateway.type;

public enum PassportAuthenticationFailureReason {
    AUTHENTICATION_REQUIRED(false),
    CREDENTIAL_CONFLICT(false),
    CREDENTIAL_INVALID(false),
    JWT_REJECTED(false),
    JWT_CLAIMS_INVALID(false),
    SESSION_MISSING(false),
    SESSION_MALFORMED(false),
    SESSION_VERSION_UNSUPPORTED(false),
    SESSION_INACTIVE(false),
    SESSION_EXPIRED(false),
    SUBJECT_MISMATCH(false),
    INSTANCE_MISMATCH(false),
    ROLE_MISMATCH(false),
    USER_VERSION_MISSING(false),
    USER_VERSION_MALFORMED(false),
    USER_VERSION_MISMATCH(false),
    ADMIN_VERSION_MISSING(false),
    ADMIN_VERSION_MALFORMED(false),
    ADMIN_VERSION_MISMATCH(false),
    REDIS_UNAVAILABLE(true),
    DEFAULT_DENY(false);

    private final boolean operationalFailure;

    PassportAuthenticationFailureReason(boolean operationalFailure) {
        this.operationalFailure = operationalFailure;
    }

    public boolean isOperationalFailure() {
        return operationalFailure;
    }

    public String metricValue() {
        return name().toLowerCase();
    }
}
