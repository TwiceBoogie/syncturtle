package com.syncturtle.platform.gateway.security.provenance;

import java.util.Map;

public final class GatewayRequestProvenancePolicy {

    private static final Map<String, String> AUDIENCE_BY_ROUTE_ID = Map.ofEntries(
            Map.entry("auth-public-credential-endpoints", "user-service"),
            Map.entry("auth-public-oauth-endpoints", "user-service"),
            Map.entry("auth-refresh-public", "user-service"),
            Map.entry("auth-admin-session-completion-public", "user-service"),
            Map.entry("auth-sign-out-public", "user-service"),
            Map.entry("auth-set-password-protected", "user-service"),
            Map.entry("auth-admin-sign-out-public-rewrite", "user-service"),
            Map.entry("user-protected", "user-service"),
            Map.entry("instance-root-public", "instance-service"),
            Map.entry("instance-admin-session-optional-auth", "instance-service"),
            Map.entry("instance-admin-sign-in-public", "instance-service"),
            Map.entry("instance-admin-sign-up-public", "instance-service"),
            Map.entry("instance-workspace-protected", "instance-service"),
            Map.entry("instance-email-credentials-check-protected", "email-service"),
            Map.entry("instance-protected", "instance-service"),
            Map.entry("instance-admin-post-protected", "instance-service"),
            Map.entry("instance-admin-patch-protected", "instance-service"),
            Map.entry("user-workspace-get-protected", "workspace-service"),
            Map.entry("workspace-protected", "workspace-service"),
            Map.entry("file-service-protected", "file-service"));

    public String requireAudience(String routeId) {
        if (routeId == null || routeId.isBlank()) {
            throw new IllegalStateException("Gateway route has no request provenance route id");
        }

        String audience = AUDIENCE_BY_ROUTE_ID.get(routeId);
        if (audience == null) {
            throw new IllegalStateException("No code-owned request provenance audience for route " + routeId);
        }

        return audience;
    }

    public int routeCount() {
        return AUDIENCE_BY_ROUTE_ID.size();
    }

}
