package com.syncturtle.services.user.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.syncturtle.services.user.configuration.UserServicePropertiesConfiguration;

class RefreshSessionLifecycleConfigurationTest {

    @Test
    void retainsAdminHandoffBindingWithoutSuccessorEnvelopeKey() {
        new ApplicationContextRunner()
                .withUserConfiguration(UserServicePropertiesConfiguration.class)
                .withPropertyValues(
                        "app.auth.refresh-session.client-binding.hmac-key=ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8",
                        "app.passport.issuer=https://api.syncturtle.test/auth",
                        "app.passport.audience=syncturtle-test",
                        "app.passport.kid=test-key",
                        "app.passport.private-key-location=file:/generated-test-keys/jwt-private-key.pem",
                        "app.passport.public-key-location=file:/generated-test-keys/jwt-public-key.pem")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(RefreshSessionClientBindingProperties.class);
                });
    }
}
