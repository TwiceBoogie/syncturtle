package com.syncturtle.services.email.service.runtime;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class EmailRuntimeConfigSnapshot {

    private final boolean enabled;
    private final String host;
    private final Integer port;
    private final String username;
    private final String password;
    private final String from;
    private final boolean useTls;
    private final boolean useSsl;
    private final long version;

    @Builder
    private EmailRuntimeConfigSnapshot(
            boolean enabled,
            String host,
            Integer port,
            String username,
            String password,
            String from,
            boolean useTls,
            boolean useSsl,
            long version) {
        Assert.isTrue(version >= 0, "version must not be negative");

        if (port != null) {
            Assert.isTrue(port >= 1 && port <= 65_535, "port must be between 1 and 65535");
        }

        this.enabled = enabled;
        this.host = normalizeOptional(host);
        this.port = port;
        this.username = normalizeOptional(username);
        this.password = normalizeSecret(password);
        this.from = normalizeOptional(from);
        this.useTls = useTls;
        this.useSsl = useSsl;
        this.version = version;
    }

    public boolean isComplete() {
        return enabled
                && host != null
                && port != null
                && from != null;
    }

    public boolean requiresAuthentication() {
        return username != null;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeSecret(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }

}
