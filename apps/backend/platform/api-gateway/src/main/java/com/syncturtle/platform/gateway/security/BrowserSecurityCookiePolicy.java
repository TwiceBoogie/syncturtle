package com.syncturtle.platform.gateway.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import com.syncturtle.common.security.cookie.SecurityCookieFactory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class BrowserSecurityCookiePolicy {

    private static final String REFRESH_PATH = "/auth/refresh";
    private static final Set<String> LOGOUT_PATHS = Set.of(
            "/auth/sign-out",
            "/auth/admin/sign-out");
    private static final String ADMIN_SESSION_COMPLETION_PATH = "/auth/admin/session";
    private static final Set<String> USER_FORM_PATHS = Set.of(
            "/auth/sign-in",
            "/auth/sign-up",
            "/auth/sign-out",
            "/auth/admin/sign-out",
            "/auth/forgot-password",
            "/auth/reset-password");
    private static final Set<String> INSTANCE_FORM_PATHS = Set.of(
            "/api/instances/admins/sign-in",
            "/api/instances/admins/sign-up");

    private final SecurityCookieFactory cookieFactory;

    public Decision decide(ServerHttpRequest request) {
        if (request == null || request.getMethod() == null || HttpMethod.OPTIONS.equals(request.getMethod())) {
            return Decision.forward(List.of());
        }

        HttpMethod method = request.getMethod();
        String path = normalizePath(request.getPath().pathWithinApplication().value());
        boolean formUrlEncoded = isFormUrlEncoded(request);
        MultiValueMap<String, HttpCookie> requestCookies = request.getCookies();
        List<HttpCookie> downstreamCookies = new ArrayList<>();

        if (HttpMethod.POST.equals(method) && REFRESH_PATH.equals(path)) {
            RefreshSelection selection = selectRefreshCookie(requestCookies);
            if (selection.isConflict()) {
                return Decision.refreshConflict();
            }

            addSelectedRefresh(downstreamCookies, selection);
            return Decision.forward(downstreamCookies);
        }

        if (HttpMethod.POST.equals(method) && LOGOUT_PATHS.contains(path)) {
            RefreshSelection selection = selectRefreshCookie(requestCookies);
            if (!selection.isConflict()) {
                addSelectedRefresh(downstreamCookies, selection);
            }

            if (formUrlEncoded) {
                addSingleCookie(downstreamCookies, requestCookies, cookieFactory.csrfCookieName());
            }

            return Decision.forward(downstreamCookies);
        }

        if (HttpMethod.GET.equals(method) && ADMIN_SESSION_COMPLETION_PATH.equals(path)) {
            addSingleCookie(
                    downstreamCookies,
                    requestCookies,
                    cookieFactory.adminSessionHandoffCookieName());
            addSingleCookie(downstreamCookies, requestCookies, cookieFactory.csrfCookieName());
            return Decision.forward(downstreamCookies);
        }

        if (HttpMethod.POST.equals(method)
                && formUrlEncoded
                && (USER_FORM_PATHS.contains(path) || INSTANCE_FORM_PATHS.contains(path))) {
            addSingleCookie(downstreamCookies, requestCookies, cookieFactory.csrfCookieName());
        }

        return Decision.forward(downstreamCookies);
    }

    private RefreshSelection selectRefreshCookie(MultiValueMap<String, HttpCookie> requestCookies) {
        List<HttpCookie> candidates = new ArrayList<>();

        for (String acceptedName : cookieFactory.acceptedRefreshCookieNames()) {
            List<HttpCookie> cookies = requestCookies.get(acceptedName);
            if (cookies != null) {
                candidates.addAll(cookies);
            }
        }

        if (candidates.isEmpty()) {
            return RefreshSelection.missing();
        }

        if (candidates.size() != 1) {
            return RefreshSelection.conflict();
        }

        String value = candidates.getFirst().getValue();
        if (!StringUtils.hasText(value)) {
            return RefreshSelection.missing();
        }

        return RefreshSelection.accepted(value.trim());
    }

    private void addSelectedRefresh(List<HttpCookie> downstreamCookies, RefreshSelection selection) {
        if (selection.getValue() != null) {
            downstreamCookies.add(new HttpCookie(cookieFactory.refreshCookieName(), selection.getValue()));
        }
    }

    private static void addSingleCookie(
            List<HttpCookie> downstreamCookies,
            MultiValueMap<String, HttpCookie> requestCookies,
            String cookieName) {
        List<HttpCookie> cookies = requestCookies.get(cookieName);
        if (cookies == null || cookies.size() != 1) {
            return;
        }

        String value = cookies.getFirst().getValue();
        if (StringUtils.hasText(value)) {
            downstreamCookies.add(new HttpCookie(cookieName, value.trim()));
        }
    }

    private static boolean isFormUrlEncoded(ServerHttpRequest request) {
        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);

        if (contentType == null) {
            return false;
        }

        return contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }

    private static String normalizePath(String rawPath) {
        String path = rawPath == null ? "" : rawPath.trim();
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    @Getter
    public static final class Decision {

        private final List<HttpCookie> downstreamCookies;
        private final boolean refreshConflict;

        private Decision(List<HttpCookie> downstreamCookies, boolean refreshConflict) {
            this.downstreamCookies = List.copyOf(downstreamCookies);
            this.refreshConflict = refreshConflict;
        }

        private static Decision forward(List<HttpCookie> downstreamCookies) {
            return new Decision(downstreamCookies, false);
        }

        private static Decision refreshConflict() {
            return new Decision(List.of(), true);
        }

    }

    @Getter
    private static final class RefreshSelection {

        private final String value;
        private final boolean conflict;

        private RefreshSelection(String value, boolean conflict) {
            this.value = value;
            this.conflict = conflict;
        }

        private static RefreshSelection missing() {
            return new RefreshSelection(null, false);
        }

        private static RefreshSelection accepted(String value) {
            return new RefreshSelection(value, false);
        }

        private static RefreshSelection conflict() {
            return new RefreshSelection(null, true);
        }

    }

}
