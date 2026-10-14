package com.syncturtle.services.user.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class UserConfigurationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
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
                assertThat(context.getBean(PassportProperties.class).getPrivateKeyLocation())
                        .isEqualTo("file:/generated-test-keys/jwt-private-key.pem");
            });
        }
    }

    @Nested
    class InvalidConfiguration {

        @Test
        void rejectsMissingPrivateKeyLocation() {
            // arrange and act
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues(
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
                            "app.passport.issuer=https://api.syncturtle.test/auth",
                            "app.passport.audience=syncturtle-test",
                            "app.passport.private-key-location=file:/generated-test-keys/jwt-private-key.pem",
                            "app.passport.public-key-location=file:/generated-test-keys/jwt-public-key.pem")
                    .run(context -> {
                        // assert
                        assertThat(context).hasFailed();
                    });
        }

    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PassportProperties.class)
    static class TestConfiguration {
    }
}
