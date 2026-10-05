package com.syncturtle.common.kafka.property;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;

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
                Duration.ofSeconds(8), true);

        assertThat(properties.getInitialInterval()).isEqualTo(Duration.ofMillis(500));
        assertThat(properties.isLogRetries()).isTrue();
    }

    @Test
    void rejectsUnboundedRetrySettings() {
        assertThatThrownBy(() -> new KafkaErrorHandlingProperties(
                Duration.ofSeconds(6), 2.0d, Duration.ofSeconds(8), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new KafkaErrorHandlingProperties(
                Duration.ofMillis(500), Double.NaN, Duration.ofSeconds(8), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new KafkaErrorHandlingProperties(
                Duration.ofMillis(500), 5.0d, Duration.ofSeconds(8), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new KafkaErrorHandlingProperties(
                Duration.ofMillis(500), 2.0d, Duration.ofMillis(100), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new KafkaErrorHandlingProperties(
                Duration.ofMillis(500), 2.0d, Duration.ofMinutes(3), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotCreateHandlerWithoutKafkaTemplate() {
        contextRunner

                .run(context -> assertThat(context).doesNotHaveBean(CommonErrorHandler.class));
    }

    @Test
    void configuresRequiredRecoveryWithoutAnEnableSwitch() {
        contextRunner
                .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(KafkaErrorHandlingProperties.class);
                    assertThat(context).hasSingleBean(CommonErrorHandler.class);
                });
    }

}
