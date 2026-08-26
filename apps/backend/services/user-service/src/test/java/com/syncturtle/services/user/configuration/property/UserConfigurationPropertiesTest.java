package com.syncturtle.services.user.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class UserConfigurationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "app.auth.token.access-token-ttl=15m",
                    "app.auth.token.password-reset-token-ttl=30m",
                    "app.auth.token.password-reset-token-bytes=48",
                    "app.passport.issuer=https://api.syncturtle.test/auth",
                    "app.passport.audience=syncturtle-test",
                    "app.passport.kid=test-key",
                    "app.passport.private-key-location=file:/generated-test-keys/jwt-private-key.pem",
                    "app.passport.public-key-location=file:/generated-test-keys/jwt-public-key.pem");

    @Nested
    class Binding {

        @Test
        void bindsExplicitOperationalValues() {
            // arrange and act
            contextRunner.run(context -> {
                // assert
                assertThat(context).hasNotFailed();
                assertThat(context.getBean(AuthProperties.class).getAccessTokenTtl())
                        .isEqualTo(Duration.ofMinutes(15));
                assertThat(context.getBean(PassportProperties.class).getPrivateKeyLocation())
                        .isEqualTo("file:/generated-test-keys/jwt-private-key.pem");
            });
        }
    }

    @Nested
    class InvalidConfiguration {

        @Test
        void rejectsMissingOperationalAuthValue() {
            // arrange and act
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues(
                            "app.auth.token.password-reset-token-ttl=30m",
                            "app.auth.token.password-reset-token-bytes=48",
                            "app.passport.issuer=https://api.syncturtle.test/auth",
                            "app.passport.audience=syncturtle-test",
                            "app.passport.kid=test-key",
                            "app.passport.private-key-location=file:/generated-test-keys/jwt-private-key.pem",
                            "app.passport.public-key-location=file:/generated-test-keys/jwt-public-key.pem")
                    .run(context -> {
                        // assert
                        assertThat(context).hasFailed();
                    });
        }

        @Test
        void rejectsMissingPrivateKeyLocation() {
            // arrange and act
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues(
                            "app.auth.token.access-token-ttl=15m",
                            "app.auth.token.password-reset-token-ttl=30m",
                            "app.auth.token.password-reset-token-bytes=48",
                            "app.passport.issuer=https://api.syncturtle.test/auth",
                            "app.passport.audience=syncturtle-test",
                            "app.passport.kid=test-key",
                            "app.passport.public-key-location=file:/generated-test-keys/jwt-public-key.pem")
                    .run(context -> {
                        // assert
                        assertThat(context).hasFailed();
                    });
        }

        @Test
        void rejectsMissingPublicKeyLocation() {
            // arrange and act
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues(
                            "app.auth.token.access-token-ttl=15m",
                            "app.auth.token.password-reset-token-ttl=30m",
                            "app.auth.token.password-reset-token-bytes=48",
                            "app.passport.issuer=https://api.syncturtle.test/auth",
                            "app.passport.audience=syncturtle-test",
                            "app.passport.kid=test-key",
                            "app.passport.private-key-location=file:/generated-test-keys/jwt-private-key.pem")
                    .run(context -> {
                        // assert
                        assertThat(context).hasFailed();
                    });
        }

        @Test
        void rejectsMissingKid() {
            // arrange and act
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues(
                            "app.auth.token.access-token-ttl=15m",
                            "app.auth.token.password-reset-token-ttl=30m",
                            "app.auth.token.password-reset-token-bytes=48",
                            "app.passport.issuer=https://api.syncturtle.test/auth",
                            "app.passport.audience=syncturtle-test",
                            "app.passport.private-key-location=file:/generated-test-keys/jwt-private-key.pem",
                            "app.passport.public-key-location=file:/generated-test-keys/jwt-public-key.pem")
                    .run(context -> {
                        // assert
                        assertThat(context).hasFailed();
                    });
        }

        @Test
        void rejectsInvalidTokenSize() {
            // arrange and act
            contextRunner
                    .withPropertyValues("app.auth.token.password-reset-token-bytes=16")
                    .run(context -> {
                        // assert
                        assertThat(context).hasFailed();
                    });
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ AuthProperties.class, PassportProperties.class })
    static class TestConfiguration {
    }
}
