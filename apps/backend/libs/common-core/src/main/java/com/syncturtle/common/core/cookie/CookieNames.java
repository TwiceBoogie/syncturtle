package com.syncturtle.common.core.cookie;

public final class CookieNames {
    private CookieNames() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String COOKIE_NAME_ACCESS_TOKEN = "access_token";
    public static final String COOKIE_NAME_REFRESH_TOKEN = "refresh_token";
    public static final String COOKIE_NAME_CSRF_TOKEN = "csrf_token";
    public static final String COOKIE_NAME_ADMIN_SESSION_HANDOFF = "admin_session_handoff";
}
