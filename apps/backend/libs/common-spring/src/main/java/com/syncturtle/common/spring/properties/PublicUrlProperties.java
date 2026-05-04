package com.syncturtle.common.spring.properties;

import java.net.URI;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import lombok.Getter;

/**
 * Public URL configuration for browser facing frontends and externally visible
 * backend/gateway endpoints.
 * 
 * <p>
 * These URLs should represent the public URLs that browsers, OAuth providers,
 * webhooks, and external clients can reach.
 * 
 * <p>
 * IMPORTANT:
 * <ul>
 * <li>api: public backend/gateway origin, used for OAuth
 * callbacks/webhooks</li>
 * <li>userApp: normal user facing frontend</li>
 * <li>admin: admin frontend</li>
 * </ul>
 */
@Getter
@ConfigurationProperties(prefix = "app.public")
public final class PublicUrlProperties {

    private final ApiRoute api;
    private final UserAppRoute userApp;
    private final AdminRoute admin;

    public PublicUrlProperties(
            @DefaultValue ApiRoute api,
            @DefaultValue UserAppRoute userApp,
            @DefaultValue AdminRoute admin) {
        this.api = Objects.requireNonNull(api, "api is required");
        this.userApp = Objects.requireNonNull(userApp, "userApp is required");
        this.admin = Objects.requireNonNull(admin, "admin is required");
    }

    @Getter
    public static final class ApiRoute {
        /**
         * Public backend/gateway origin.
         * 
         * <p>
         * Examples:
         * - http://localhost:8000
         * - https://api.syncturtle.com
         * - https://syncturtle.example.com
         */
        private final String origin;
        /**
         * Optional public API base path.
         * 
         * <p>
         * Examples:
         * - empty
         * - /api/
         * - /backend/
         */
        private final String basePath;

        public ApiRoute(
                @DefaultValue("http://localhost:8000") String origin,
                @DefaultValue("") String basePath) {
            this.origin = normalizeRequiredOrigin(origin, "api.origin");
            this.basePath = normalizeBasePath(basePath, "api.basePath", true);
        }
    }

    @Getter
    public static final class UserAppRoute {
        /**
         * Public origin for the normal user facing frontend
         */
        private final String origin;
        /**
         * Public base path for the normal user facing frontend
         */
        private final String basePath;

        public UserAppRoute(
                @DefaultValue("http://localhost:3000") String origin,
                @DefaultValue("/") String basePath) {
            this.origin = normalizeRequiredOrigin(origin, "userApp.origin");
            this.basePath = normalizeBasePath(basePath, "userApp.basePath", true);
        }
    }

    @Getter
    public static final class AdminRoute {

        /**
         * Public origin for the admin frontend
         */
        private final String origin;
        /**
         * Public admin base path
         */
        private final String basePath;

        public AdminRoute(
                @DefaultValue("http://localhost:3001") String origin,
                @DefaultValue("/god-mode/") String basePath) {
            this.origin = normalizeRequiredOrigin(origin, "admin.origin");
            this.basePath = normalizeBasePath(basePath, "admin.basePath", true);
        }
    }

    private static String normalizeRequiredOrigin(String raw, String propertyName) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException(propertyName + " is required");
        }

        return normalizeOrigin(raw, propertyName);
    }

    private static String normalizeOrigin(String raw, String propertyName) {
        String value = raw.trim();
        URI uri = URI.create(value);

        if (!StringUtils.hasText(uri.getScheme())) {
            throw new IllegalArgumentException(propertyName + " must include a scheme, such as http or https");
        }

        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(propertyName + " must use http or https");
        }

        if (!StringUtils.hasText(uri.getHost())) {
            throw new IllegalArgumentException(propertyName + " must include a host");
        }

        if (StringUtils.hasText(uri.getQuery())) {
            throw new IllegalArgumentException(propertyName + " must not include a query string");
        }

        if (StringUtils.hasText(uri.getFragment())) {
            throw new IllegalArgumentException(propertyName + " must not include a fragment");
        }

        String path = uri.getPath();
        if (StringUtils.hasText(path) && !"/".equals(path)) {
            throw new IllegalArgumentException(propertyName + " must be an origin only. Put path values in basePath");
        }

        return trimTrailingSlash(value);
    }

    private static String normalizeBasePath(String raw, String propertyName, boolean allowEmpty) {
        if (!StringUtils.hasText(raw)) {
            return allowEmpty ? "" : "/";
        }

        String value = raw.trim();

        if (value.contains("?")) {
            throw new IllegalArgumentException(propertyName + " must not inlcude a query string");
        }

        if (value.contains("#")) {
            throw new IllegalArgumentException(propertyName + " must not include a fragment");
        }

        if (value.contains("..")) {
            throw new IllegalArgumentException(propertyName + " must not contain path traversal segments");
        }

        if (!value.startsWith("/")) {
            value = "/" + value;
        }

        if (!value.endsWith("/")) {
            value = value + "/";
        }

        return value;
    }

    private static String trimTrailingSlash(String value) {
        String out = value.trim();

        while (out.endsWith("/") && out.length() > 1) {
            out = out.substring(0, out.length() - 1);
        }

        return out;
    }

}
