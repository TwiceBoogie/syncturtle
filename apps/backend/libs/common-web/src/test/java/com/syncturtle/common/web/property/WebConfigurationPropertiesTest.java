package com.syncturtle.common.web.property;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class WebConfigurationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsNestedPublicUrlDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(PublicUrlProperties.class).getApi().getOrigin())
                    .isEqualTo("http://localhost:8000");
        });
    }

    @Test
    void rejectsErrorStackTraceWithoutDebugDetails() {
        contextRunner
                .withPropertyValues(
                        "app.web.errors.include-debug=false",
                        "app.web.errors.include-stack-trace=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            ErrorResponseProperties.class,
            GatewayContextProperties.class,
            PublicUrlProperties.class
    })
    static class TestConfiguration {
    }
}
