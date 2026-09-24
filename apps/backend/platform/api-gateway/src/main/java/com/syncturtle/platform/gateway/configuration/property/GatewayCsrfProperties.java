package com.syncturtle.platform.gateway.configuration.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.gateway.csrf")
public final class GatewayCsrfProperties {

    private static final int MIN_NONCE_BYTES = 32;

    private final Duration preAuthLifetime;
    private final Duration sessionMaxLifetime;
    private final int nonceBytes;
    private final int maxSignedTokenChars;
    private final int maxPayloadBytes;

    public GatewayCsrfProperties(
            @DefaultValue("30m") Duration preAuthLifetime,
            @DefaultValue("7d") Duration sessionMaxLifetime,
            @DefaultValue("32") int nonceBytes,
            @DefaultValue("2048") int maxSignedTokenChars,
            @DefaultValue("1024") int maxPayloadBytes) {
        Assert.notNull(preAuthLifetime, "pre-auth-lifetime is required");
        Assert.isTrue(preAuthLifetime.isPositive(), "pre-auth-lifetime must be positive");
        Assert.notNull(sessionMaxLifetime, "session-max-lifetime is required");
        Assert.isTrue(sessionMaxLifetime.isPositive(), "session-max-lifetime must be positive");
        Assert.isTrue(nonceBytes >= MIN_NONCE_BYTES, "nonce-bytes must be >= 32");
        Assert.isTrue(maxPayloadBytes >= 256, "max-payload-bytes must be >= 256");
        Assert.isTrue(maxSignedTokenChars > maxPayloadBytes, "max-signed-token-chars must exceed max-payload-bytes");

        this.preAuthLifetime = preAuthLifetime;
        this.sessionMaxLifetime = sessionMaxLifetime;
        this.nonceBytes = nonceBytes;
        this.maxSignedTokenChars = maxSignedTokenChars;
        this.maxPayloadBytes = maxPayloadBytes;
    }

}
