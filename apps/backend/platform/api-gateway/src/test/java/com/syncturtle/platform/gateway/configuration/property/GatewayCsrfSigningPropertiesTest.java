package com.syncturtle.platform.gateway.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class GatewayCsrfSigningPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Nested
    class ConstructorTests {

        @Test
        void retainsTheConfiguredKey() {
            GatewayCsrfSigningProperties properties = new GatewayCsrfSigningProperties(" canonical-key ");

            assertThat(properties.getKey()).isEqualTo("canonical-key");
        }

        @Test
        void rejectsMissingKey() {
            assertThatThrownBy(() -> new GatewayCsrfSigningProperties(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new GatewayCsrfSigningProperties(" "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

    @Nested
    class BindingTests {

        @Test
        void bindsTheGatewayOwnedKey() {
            contextRunner.withPropertyValues("app.gateway.csrf.signing.key=canonical-key")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getBean(GatewayCsrfSigningProperties.class).getKey())
                                .isEqualTo("canonical-key");
                    });
        }

        @Test
        void failsWithoutTheGatewayOwnedKey() {
            contextRunner.run(context -> assertThat(context).hasFailed());
        }

    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(GatewayCsrfSigningProperties.class)
    static class TestConfiguration {

    }

}
