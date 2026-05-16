package com.syncturtle.services.instance.configurations.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.web.authorization")
public final class AuthorizationInterceptorProperties {

    private static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 100;
    private static final List<String> DEFAULT_PATH_PATTERNS = List.of("/api/**");
    private static final List<String> DEFAULT_EXCLUDE_PATH_PATTERNS = List.of(
            "/actuator/**",
            "/error");

    private final boolean enabled;
    private final int order;
    private final boolean requireAuthenticationByDefault;
    private final List<String> pathPatterns;
    private final List<String> excludePathPatterns;

    public AuthorizationInterceptorProperties(
            @DefaultValue("true") boolean enabled,
            Integer order,
            @DefaultValue("false") boolean requireAuthenticationByDefault,
            List<String> pathPatterns,
            List<String> excludePathPatterns) {
        this.enabled = enabled;
        this.order = order != null ? order : DEFAULT_ORDER;
        this.requireAuthenticationByDefault = requireAuthenticationByDefault;
        this.pathPatterns = normalizePatterns(pathPatterns, DEFAULT_PATH_PATTERNS, "path-patterns");
        this.excludePathPatterns = normalizePatterns(excludePathPatterns, DEFAULT_EXCLUDE_PATH_PATTERNS,
                "exclude-path-patterns");
    }

    private static List<String> normalizePatterns(
            List<String> patterns,
            List<String> defaults,
            String propertyName) {
        List<String> resolved = patterns == null || patterns.isEmpty()
                ? defaults
                : patterns;

        return resolved.stream()
                .map(String::trim)
                .peek(pattern -> validatePattern(pattern, propertyName))
                .distinct()
                .toList();
    }

    private static void validatePattern(String pattern, String propertyName) {
        if (!StringUtils.hasText(pattern)) {
            throw new IllegalArgumentException(
                    "app.web.authorization." + propertyName + " must not contain blank values");
        }
        if (!pattern.startsWith("/")) {
            throw new IllegalArgumentException(
                    "app.web.authorization." + propertyName + " value must start with '/': " + pattern);
        }
    }

}
