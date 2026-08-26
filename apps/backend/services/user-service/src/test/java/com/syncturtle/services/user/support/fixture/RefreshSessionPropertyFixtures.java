package com.syncturtle.services.user.support.fixture;

import java.time.Duration;

import com.syncturtle.services.user.configuration.property.RefreshSessionClientBindingProperties;
import com.syncturtle.services.user.configuration.property.RefreshSessionLifecycleProperties;
import com.syncturtle.services.user.configuration.property.RefreshSessionSuccessorEnvelopeProperties;

public final class RefreshSessionPropertyFixtures {

    private static final String ENVELOPE_KEY = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA";
    private static final String CLIENT_BINDING_KEY = "ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8";

    public static RefreshSessionLifecycleProperties lifecycleProperties() {
        return lifecycleProperties(Duration.ofDays(7), Duration.ofDays(30), Duration.ofSeconds(5));
    }

    public static RefreshSessionLifecycleProperties lifecycleProperties(
            Duration idleLifetime,
            Duration absoluteLifetime,
            Duration graceWindow) {
        return new RefreshSessionLifecycleProperties(
                idleLifetime,
                absoluteLifetime,
                graceWindow,
                successorEnvelopeProperties(),
                clientBindingProperties());
    }

    public static RefreshSessionSuccessorEnvelopeProperties successorEnvelopeProperties() {
        return new RefreshSessionSuccessorEnvelopeProperties(ENVELOPE_KEY);
    }

    public static RefreshSessionClientBindingProperties clientBindingProperties() {
        return new RefreshSessionClientBindingProperties(CLIENT_BINDING_KEY);
    }

    private RefreshSessionPropertyFixtures() {
    }
}
