package com.syncturtle.common.kafka.property;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.syncturtle.common.kafka.autoconfigure.KafkaErrorHandlingAutoConfiguration;

class KafkaErrorHandlingPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaErrorHandlingAutoConfiguration.class));

    @Test
    void acceptsValidExplicitConfiguration() {
        KafkaErrorHandlingProperties properties = new KafkaErrorHandlingProperties(Duration.ofMillis(500), 2.0,
                Duration.ofSeconds(8), ".DLT", true, true, true);

        assertThat(properties.getInitialInterval()).isEqualTo(Duration.ofMillis(500));
        assertThat(properties.getDltSuffix()).isEqualTo(".DLT");
    }

    @Test
    void disabledConfigurationDoesNotRegisterProperties() {
        contextRunner
                .withPropertyValues("app.kafka.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(KafkaErrorHandlingProperties.class));
    }

    @Test
    void enabledConfigurationRegistersPropertiesWithDefaults() {
        contextRunner
                .withPropertyValues("app.kafka.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(KafkaErrorHandlingProperties.class);
                });
    }

}
