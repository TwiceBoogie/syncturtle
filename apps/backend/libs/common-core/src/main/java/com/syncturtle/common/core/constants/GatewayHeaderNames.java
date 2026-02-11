package com.syncturtle.common.core.constants;

/**
 * Constants to be used between the api-gateway and downstream services.
 * 
 * - HDR_* custom headers api-gateway sends downstream
 * - HDR_INTERNAL_* custom headers services upstream to api-gateway
 */
public final class GatewayHeaderNames {
    public static final String HDR_AUTH_USER_ID = "X-Auth-User-Id";
    public static final String HDR_AUTH_SESSION_ID = "X-Auth-Session-Id";
    public static final String HDR_AUTH_SESSION_TYPE = "X-Auth-Session-Type";
    public static final String HDR_AUTH_WORKSPACE_ID = "X-Auth-Workspace-Id";
    // client -> api-gateway
    public static final String HDR_REQUEST_ID = "X-Request-Id";
    public static final String HDR_CORRELATION_ID = "X-Correlation-Id";
    // client metadata
    public static final String HDR_CLIENT_IP = "X-ST-Client-Ip";
    public static final String HDR_CLIENT_UA = "X-ST-Client-User-Agent";
    public static final String HDR_CLIENT_DEVICE_ID = "X-ST-Client-Device-Id";
    public static final String HDR_CLIENT_LOCALE = "X-ST-Client-Locale";

    public static final String HDR_INTERNAL_USER_ID = "X-ST-Internal-UserId";
    public static final String HDR_INTERNAL_SESSION_TYPE = "X-ST-Internal-SessionType";
    public static final String HDR_INTERNAL_ROLES = "X-ST-Internal-Roles";
    public static final String HDR_INTERNAL_LOGIN_CONTEXT = "X-ST-Internal-Login-CTX";
    public static final String HDR_INTERNAL_LOGOUT_CONTEXT = "X-ST-Internal-Logout-CTX";
    public static final String HDR_INTERNAL_AUTH_STATUS = "X-ST-Internal-Auth-Status";

    private GatewayHeaderNames() {
    }
}
