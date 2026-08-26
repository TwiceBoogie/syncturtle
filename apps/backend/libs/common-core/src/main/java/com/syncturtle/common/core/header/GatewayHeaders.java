package com.syncturtle.common.core.header;

/**
 * Constants to be used between the api-gateway and downstream services.
 * 
 * - HDR_* custom headers api-gateway sends downstream
 * - HDR_INTERNAL_* custom headers services upstream to api-gateway
 */
public final class GatewayHeaders {
    private GatewayHeaders() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String HDR_AUTH_USER_ID = "X-Auth-User-Id";
    public static final String HDR_AUTH_SESSION_ID = "X-Auth-Session-Id";
    public static final String HDR_AUTH_INSTANCE_ID = "X-Auth-Instance-Id";
    public static final String HDR_AUTH_ROLES = "X-Auth-Roles";
    public static final String HDR_AUTH_USER_AUTH_VERSION = "X-Auth-User-Auth-Version";
    public static final String HDR_AUTH_ADMIN_SESSION_VERSION = "X-Auth-Admin-Session-Version";
    public static final String HDR_AUTH_ISSUER = "X-Auth-Issuer";
    public static final String HDR_AUTH_WORKSPACE_ID = "X-Auth-Workspace-Id";
    // client metadata
    public static final String HDR_CLIENT_IP = "X-ST-Client-Ip";
    public static final String HDR_CLIENT_UA = "X-ST-Client-User-Agent";
    public static final String HDR_CLIENT_DEVICE_ID = "X-ST-Client-Device-Id";
    public static final String HDR_CLIENT_LOCALE = "X-ST-Client-Locale";

    public static final String HDR_PREAUTH_TRANSACTION_BINDING = "X-ST-Preauth-Transaction-Binding";
    public static final String HDR_INTERNAL_LOGIN_CONTEXT = "X-ST-Internal-Login-CTX";
    public static final String HDR_INTERNAL_LOGOUT_CONTEXT = "X-ST-Internal-Logout-CTX";
}
