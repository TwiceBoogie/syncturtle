package com.syncturtle.common.security.properties;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.security.cookies")
public final class SecurityCookieProperties {

    private final boolean secure;
    private final boolean hostPrefixEnabled;
    private final String domain;
    private final String path;
    private final String accessSameSite;
    private final String refreshSameSite;
    private final String csrfSameSite;
    private final Duration csrfMaxAge;

    public SecurityCookieProperties(
            @DefaultValue("false") boolean secure,
            @DefaultValue("false") boolean hostPrefixEnabled,
            @DefaultValue("") String domain,
            @DefaultValue("/") String path,
            @DefaultValue("Lax") String accessSameSite,
            @DefaultValue("Lax") String refreshSameSite,
            @DefaultValue("Lax") String csrfSameSite,
            @DefaultValue("30m") Duration csrfMaxAge) {
        this.secure = secure;
        this.hostPrefixEnabled = hostPrefixEnabled;
        this.domain = normalizeDomain(domain);
        this.path = normalizePath(path);
        this.accessSameSite = normalizeSameSite(accessSameSite, "accessSameSite");
        this.refreshSameSite = normalizeSameSite(refreshSameSite, "refreshSameSite");
        this.csrfSameSite = normalizeSameSite(csrfSameSite, "csrfSameSite");
        this.csrfMaxAge = requirePositive(csrfMaxAge, "csrf-max-age");

        validateHostPrefix();
        validateSameSiteNone();
    }

    public boolean hasDomain() {
        return StringUtils.hasText(domain);
    }

    public boolean shouldUseHostPrefix() {
        return secure && hostPrefixEnabled && !hasDomain() && "/".equals(path);
    }

    private void validateHostPrefix() {
        if (!hostPrefixEnabled) {
            return;
        }
        if (!secure) {
            throw new IllegalArgumentException(
                    "app.security.cookies.host-prefix-enabled requires secure=true");
        }
        if (hasDomain()) {
            throw new IllegalArgumentException(
                    "app.security.cookies.host-prefix-enabled cannot be true when domain is set");
        }
        if (!"/".equals(path)) {
            throw new IllegalArgumentException(
                    "app.security.cookies.host-prefix-enabled requires path=/");
        }
    }

    private void validateSameSiteNone() {
        if (!secure && (usesSameSiteNone(accessSameSite)
                || usesSameSiteNone(refreshSameSite)
                || usesSameSiteNone(csrfSameSite))) {
            throw new IllegalArgumentException("SameSite=None requires secure cookies");
        }
    }

    private static Duration requirePositive(Duration value, String propertyName) {
        Duration duration = Objects.requireNonNull(value, propertyName + " is required");

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    "app.security.cookies." + propertyName + " must be positive");
        }

        return duration;
    }

    private static String normalizeDomain(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }

        String value = raw.trim();

        while (value.startsWith(".")) {
            value = value.substring(1);
        }

        return value;
    }

    private static String normalizePath(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "/";
        }

        String value = raw.trim();

        if (!value.startsWith("/")) {
            value = "/" + value;
        }

        return value;
    }

    private static String normalizeSameSite(String raw, String propertyName) {
        if (!StringUtils.hasText(raw)) {
            return "Lax";
        }

        String value = raw.trim().toLowerCase(Locale.ROOT);

        return switch (value) {
            case "lax" -> "Lax";
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> throw new IllegalArgumentException(
                    "Invalid SameSite value for " + propertyName + ": " + raw);
        };
    }

    private static boolean usesSameSiteNone(String sameSite) {
        return "None".equalsIgnoreCase(sameSite);
    }

}
