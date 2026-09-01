package com.syncturtle.platform.gateway.security.csrf;

import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.syncturtle.platform.gateway.type.GatewayCsrfRequirement;

@Component
public final class GatewayCsrfRoutePolicy {

    private static final String SESSION_ROOT = "/api/users/me/sessions";

    private static final Set<String> PREAUTH_POST_PATHS = Set.of(
            "/auth/email-check",
            "/auth/magic-generate",
            "/auth/sign-in",
            "/auth/sign-up",
            "/auth/magic-sign-in",
            "/auth/magic-sign-up",
            "/auth/forgot-password",
            "/api/instances/admins/sign-in",
            "/api/instances/admins/sign-up");

    private static final Set<String> TRANSPORT_SESSION_POST_PATHS = Set.of(
            "/auth/refresh",
            "/auth/sign-out",
            "/auth/admin/sign-out");

    private static final Set<String> SESSION_POST_PATHS = Set.of(
            "/auth/set-password",
            "/api/instances/admins",
            "/api/instances/workspaces",
            "/api/instances/email-credentials-check",
            "/api/workspaces",
            "/api/assets/v1/uploads");

    private static final Set<String> SESSION_PATCH_PATHS = Set.of(
            "/api/users/me",
            "/api/users/me/profile",
            "/api/users/me/onboard",
            "/api/users/me/profile/tour-completed",
            "/api/users/me/avatar",
            "/api/users/me/cover-image",
            "/api/instances",
            "/api/instances/configurations");

    private static final Set<String> SESSION_DELETE_PATHS = Set.of(
            "/api/users/me/avatar",
            "/api/users/me/cover-image",
            SESSION_ROOT,
            SESSION_ROOT + "/others",
            "/api/instances/configurations/disable-email-feature");

    public GatewayCsrfRequirement requirement(HttpMethod method, String rawPath) {
        if (method == null || rawPath == null) {
            return GatewayCsrfRequirement.NONE;
        }

        String path = normalize(rawPath);

        if (HttpMethod.POST.equals(method)) {
            if (PREAUTH_POST_PATHS.contains(path) || isResetPasswordCompletion(path)) {
                return GatewayCsrfRequirement.PREAUTH;
            }

            if (TRANSPORT_SESSION_POST_PATHS.contains(path)) {
                return GatewayCsrfRequirement.TRANSPORT_SESSION;
            }

            if (SESSION_POST_PATHS.contains(path)
                    || isWorkspaceInvitation(path)
                    || isAssetCompletion(path)) {
                return GatewayCsrfRequirement.AUTHENTICATED_SESSION;
            }
        }

        if (HttpMethod.PATCH.equals(method)) {
            if (SESSION_PATCH_PATHS.contains(path) || isWorkspaceLogo(path)) {
                return GatewayCsrfRequirement.AUTHENTICATED_SESSION;
            }
        }

        if (HttpMethod.DELETE.equals(method)) {
            if (SESSION_DELETE_PATHS.contains(path)
                    || isCanonicalSessionDelete(path)
                    || isInstanceAdminDelete(path)
                    || isWorkspaceLogo(path)
                    || isAssetDelete(path)) {
                return GatewayCsrfRequirement.AUTHENTICATED_SESSION;
            }
        }
        return GatewayCsrfRequirement.NONE;
    }

    public boolean isAdminHandoffStart(HttpMethod method, String rawPath) {
        if (!HttpMethod.POST.equals(method)) {
            return false;
        }

        String path = normalize(rawPath);
        return path.equals("/api/instances/admins/sign-in")
                || path.equals("/api/instances/admins/sign-up");
    }

    private static boolean isResetPasswordCompletion(String path) {
        String prefix = "/auth/reset-password/";
        if (!path.startsWith(prefix)) {
            return false;
        }

        String remainder = path.substring(prefix.length());
        int separator = remainder.indexOf('/');
        return separator > 0 && separator < remainder.length() - 1 && remainder.indexOf('/', separator + 1) < 0;
    }

    private static boolean isWorkspaceInvitation(String path) {
        return hasOneSegmentBetween(path, "/api/workspaces/", "/invitations");
    }

    private static boolean isWorkspaceLogo(String path) {
        return hasOneSegmentBetween(path, "/api/workspaces/", "/logo");
    }

    private static boolean isAssetCompletion(String path) {
        return hasOneSegmentBetween(path, "/api/assets/v1/uploads/", "/complete");
    }

    private static boolean isAssetDelete(String path) {
        return hasOneTrailingSegment(path, "/api/assets/v1/");
    }

    private static boolean isInstanceAdminDelete(String path) {
        return hasOneTrailingSegment(path, "/api/instances/admins/");
    }

    private static boolean isCanonicalSessionDelete(String path) {
        String prefix = SESSION_ROOT + "/";
        if (!hasOneTrailingSegment(path, prefix)) {
            return false;
        }

        String value = path.substring(prefix.length());
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean hasOneTrailingSegment(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return false;
        }

        String segment = path.substring(prefix.length());
        return !segment.isBlank() && segment.indexOf('/') < 0;
    }

    private static boolean hasOneSegmentBetween(String path, String prefix, String suffix) {
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return false;
        }

        String segment = path.substring(prefix.length(), path.length() - suffix.length());
        return !segment.isBlank() && segment.indexOf('/') < 0;
    }

    private static String normalize(String rawPath) {
        String path = rawPath.trim();
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

}
