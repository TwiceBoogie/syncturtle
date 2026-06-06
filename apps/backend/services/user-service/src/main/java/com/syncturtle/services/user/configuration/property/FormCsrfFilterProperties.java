package com.syncturtle.services.user.configuration.property;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.security.csrf.form")
public final class FormCsrfFilterProperties {

    private static final List<String> DEFAULT_PROTECTED_ENDPOINTS = List.of(
            "/auth/sign-in",
            "/auth/sign-up",
            "/auth/sign-out",
            "/auth/forgot-password",
            "/auth/reset-password");
    private static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 50;

    private final boolean enabled;
    private final int order;
    private final List<String> protectedEndpoints;

    public FormCsrfFilterProperties(
            @DefaultValue("true") boolean enabled,
            Integer order,
            List<String> protectedEndpoints) {
        this.enabled = enabled;
        this.order = order != null ? order : DEFAULT_ORDER;
        this.protectedEndpoints = normalizeProtectedEndpoints(protectedEndpoints);
    }

    private static List<String> normalizeProtectedEndpoints(List<String> protectedEndpoints) {
        List<String> endpoints = protectedEndpoints == null || protectedEndpoints.isEmpty()
                ? DEFAULT_PROTECTED_ENDPOINTS
                : protectedEndpoints;

        return endpoints.stream()
                .map(String::trim)
                .peek(FormCsrfFilterProperties::validateEndpoint)
                .distinct()
                .toList();
    }

    private static void validateEndpoint(String endpoint) {
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException("CSRF protected endpoints must not be blank");
        }

        if (!endpoint.startsWith("/")) {
            throw new IllegalArgumentException("CSRF protected endpoint must start with '/': " + endpoint);
        }
    }

}
