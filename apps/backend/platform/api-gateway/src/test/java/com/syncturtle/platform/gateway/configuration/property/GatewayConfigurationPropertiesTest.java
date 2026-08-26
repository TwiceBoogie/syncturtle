package com.syncturtle.platform.gateway.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class GatewayConfigurationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "app.passport.gateway.issuer=https://api.syncturtle.test/auth",
                    "app.passport.gateway.audience=syncturtle-test");

    @Nested
    class Binding {

        @Test
        void bindsDefaultsAndOverrides() {
            contextRunner
                    .withPropertyValues("app.gateway.client-metadata.trusted-proxy-count=2")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBean(GatewayClientMetadataProperties.class)
                                .getTrustedProxyCount()).isEqualTo(2);
                        assertThat(context.getBean(GatewayPassportProperties.class)
                                .getAuthHeaderPrefix()).isEqualTo("X-Auth-");
                    });
        }
    }

    @Nested
    class InvalidConfiguration {

        @Test
        void rejectsNegativeTrustedProxyCount() {
            contextRunner
                    .withPropertyValues("app.gateway.client-metadata.trusted-proxy-count=-1")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void rejectsMissingIssuer() {
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfiguration.class)
                    .withPropertyValues("app.passport.gateway.audience=syncturtle-test")
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            GatewayClientMetadataProperties.class,
            GatewayPassportProperties.class
    })
    static class TestConfiguration {
    }
}
