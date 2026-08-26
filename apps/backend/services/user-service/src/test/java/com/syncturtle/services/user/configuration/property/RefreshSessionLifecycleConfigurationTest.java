package com.syncturtle.services.user.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.syncturtle.services.user.configuration.UserServicePropertiesConfiguration;

class RefreshSessionLifecycleConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(UserServicePropertiesConfiguration.class)
            .withPropertyValues(
                    "app.auth.token.access-token-ttl=15m",
                    "app.auth.token.password-reset-token-ttl=30m",
                    "app.auth.token.password-reset-token-bytes=48",
                    "app.auth.refresh-session.idle-lifetime=7d",
                    "app.auth.refresh-session.absolute-lifetime=30d",
                    "app.auth.refresh-session.grace-window=5s",
                    "app.auth.refresh-session.successor-envelope.encryption-key=AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA",
                    "app.auth.refresh-session.client-binding.hmac-key=ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8",
                    "app.passport.issuer=https://api.syncturtle.test/auth",
                    "app.passport.audience=syncturtle-test",
                    "app.passport.kid=test-key",
                    "app.passport.private-key-location=file:/generated-test-keys/jwt-private-key.pem",
                    "app.passport.public-key-location=file:/generated-test-keys/jwt-public-key.pem");

    @Nested
    class Binding {

        @Test
        void registersAndBindsApprovedLifecycleValues() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();
                RefreshSessionLifecycleProperties result = context.getBean(
                        RefreshSessionLifecycleProperties.class);
                assertThat(result.getIdleLifetime()).isEqualTo(Duration.ofDays(7));
                assertThat(result.getAbsoluteLifetime()).isEqualTo(Duration.ofDays(30));
                assertThat(result.getGraceWindow()).isEqualTo(Duration.ofSeconds(5));
                assertThat(context).hasSingleBean(RefreshSessionSuccessorEnvelopeProperties.class);
                assertThat(context).hasSingleBean(RefreshSessionClientBindingProperties.class);
            });
        }
    }

    @Nested
    class InvalidConfiguration {

        @Test
        void rejectsMissingIdleLifetime() {
            new ApplicationContextRunner()
                    .withUserConfiguration(UserServicePropertiesConfiguration.class)
                    .withPropertyValues(
                            "app.auth.token.access-token-ttl=15m",
                            "app.auth.token.password-reset-token-ttl=30m",
                            "app.auth.token.password-reset-token-bytes=48",
                            "app.auth.refresh-session.absolute-lifetime=30d",
                            "app.auth.refresh-session.grace-window=5s",
                            "app.auth.refresh-session.successor-envelope.encryption-key=AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA",
                            "app.auth.refresh-session.client-binding.hmac-key=ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8",
                            "app.passport.issuer=https://api.syncturtle.test/auth",
                            "app.passport.audience=syncturtle-test",
                            "app.passport.kid=test-key",
                            "app.passport.private-key-location=file:/generated-test-keys/jwt-private-key.pem",
                            "app.passport.public-key-location=file:/generated-test-keys/jwt-public-key.pem")
                    .run(context -> assertThat(context).hasFailed());
        }
    }
}
