package com.syncturtle.services.user.configuration.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.auth.admin-session-handoff")
public final class AdminSessionHandoffProperties {

    public static final Duration MAXIMUM_TTL = Duration.ofSeconds(30);

    private final Duration ttl;

    public AdminSessionHandoffProperties(@DefaultValue("30s") Duration ttl) {
        Assert.notNull(ttl, "admin session handoff ttl is required");

        this.ttl = ttl;
        if (ttl.isZero() || ttl.isNegative() || ttl.compareTo(MAXIMUM_TTL) > 0) {
            throw new IllegalArgumentException("app.auth.admin-session-handoff.ttl must be between 1ms and 30s");
        }
    }

}
