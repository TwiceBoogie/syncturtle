package com.syncturtle.common.security.property;

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
    private final boolean managedPrefixEnabled;
    private final String accessSameSite;
    private final String refreshSameSite;
    private final String csrfSameSite;
    private final Duration csrfMaxAge;

    public SecurityCookieProperties(
            @DefaultValue("false") boolean secure,
            @DefaultValue("false") boolean managedPrefixEnabled,
            @DefaultValue("Lax") String accessSameSite,
            @DefaultValue("Lax") String refreshSameSite,
            @DefaultValue("Lax") String csrfSameSite,
            @DefaultValue("30m") Duration csrfMaxAge) {
        this.secure = secure;
        this.managedPrefixEnabled = managedPrefixEnabled;
        this.accessSameSite = normalizeSameSite(accessSameSite, "accessSameSite");
        this.refreshSameSite = normalizeSameSite(refreshSameSite, "refreshSameSite");
        this.csrfSameSite = normalizeSameSite(csrfSameSite, "csrfSameSite");
        this.csrfMaxAge = requirePositive(csrfMaxAge, "csrf-max-age");

        validateManagedPrefix();
        validateSameSiteNone();
    }

    public boolean shouldUseManagedPrefix() {
        return secure && managedPrefixEnabled;
    }

    private void validateManagedPrefix() {
        if (managedPrefixEnabled && !secure) {
            throw new IllegalArgumentException(
                    "app.security.cookies.managed-prefix-enabled requires secure=true");
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
