package com.syncturtle.services.user.configuration.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.auth.refresh-session", ignoreUnknownFields = false)
public final class RefreshSessionLifecycleProperties {

    private final Duration idleLifetime;
    private final Duration absoluteLifetime;
    private final Duration graceWindow;
    private final RefreshSessionSuccessorEnvelopeProperties successorEnvelope;
    private final RefreshSessionClientBindingProperties clientBinding;

    public RefreshSessionLifecycleProperties(
            Duration idleLifetime,
            Duration absoluteLifetime,
            Duration graceWindow,
            RefreshSessionSuccessorEnvelopeProperties successorEnvelope,
            RefreshSessionClientBindingProperties clientBinding) {
        this.idleLifetime = requirePositive(idleLifetime, "idle-lifetime");
        this.absoluteLifetime = requirePositive(absoluteLifetime, "absolute-lifetime");
        this.graceWindow = requirePositive(graceWindow, "grace-window");
        this.successorEnvelope = requireNested(successorEnvelope, "successor-envelope");
        this.clientBinding = requireNested(clientBinding, "client-binding");

        if (!this.idleLifetime.equals(RefreshSessionFamilyRecord.IDLE_LIFETIME)) {
            throw new IllegalArgumentException(
                    "app.auth.refresh-session.idle-lifetime must be 7 days");
        }
        if (!this.absoluteLifetime.equals(RefreshSessionFamilyRecord.ABSOLUTE_LIFETIME)) {
            throw new IllegalArgumentException(
                    "app.auth.refresh-session.absolute-lifetime must be 30 days");
        }
        if (!this.graceWindow.equals(Duration.ofSeconds(5))) {
            throw new IllegalArgumentException(
                    "app.auth.refresh-session.grace-window must be 5 seconds");
        }
    }

    private static Duration requirePositive(Duration value, String propertyName) {
        if (value == null) {
            throw new IllegalArgumentException("app.auth.refresh-session." + propertyName + " is required");
        }
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(
                    "app.auth.refresh-session." + propertyName + " must be positive");
        }
        return value;
    }

    private static <T> T requireNested(T value, String propertyName) {
        if (value == null) {
            throw new IllegalArgumentException("app.auth.refresh-session." + propertyName + " is required");
        }
        return value;
    }

}
