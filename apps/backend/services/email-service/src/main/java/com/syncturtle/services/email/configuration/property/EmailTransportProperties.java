package com.syncturtle.services.email.configuration.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.email.transport", ignoreUnknownFields = false)
public final class EmailTransportProperties {

    private static final Duration MAX_JAVA_MAIL_TIMEOUT = Duration.ofMillis(Integer.MAX_VALUE);

    private final Duration connectionTimeout;
    private final Duration readTimeout;
    private final Duration writeTimeout;

    public EmailTransportProperties(
            @DefaultValue("5s") Duration connectionTimeout,
            @DefaultValue("10s") Duration readTimeout,
            @DefaultValue("10s") Duration writeTimeout) {
        this.connectionTimeout = requireJavaMailTimeout(connectionTimeout, "app.email.transport.connection-timeout");
        this.readTimeout = requireJavaMailTimeout(readTimeout, "app.email.transport.read-timeout");
        this.writeTimeout = requireJavaMailTimeout(writeTimeout, "app.email.transport.write-timeout");
    }

    public int getConnectionTimeoutMillis() {
        return Math.toIntExact(connectionTimeout.toMillis());
    }

    public int getReadTimeoutMillis() {
        return Math.toIntExact(readTimeout.toMillis());
    }

    public int getWriteTimeoutMillis() {
        return Math.toIntExact(writeTimeout.toMillis());
    }

    private static Duration requireJavaMailTimeout(Duration duration, String propertyName) {
        Assert.notNull(duration, propertyName + " is required");
        Assert.isTrue(!duration.isNegative() && !duration.isZero(), propertyName + " must be positive");
        Assert.isTrue(duration.compareTo(MAX_JAVA_MAIL_TIMEOUT) <= 0, propertyName + " is too large");

        return duration;
    }

}
