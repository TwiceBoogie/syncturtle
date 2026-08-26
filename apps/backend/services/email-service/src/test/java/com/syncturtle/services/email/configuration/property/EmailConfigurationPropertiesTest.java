package com.syncturtle.services.email.configuration.property;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.context.annotation.Configuration;

class EmailConfigurationPropertiesTest {

    private static final String[] REQUIRED_PROPERTIES = {
            "app.kafka.enabled=false",
            "app.kafka.consumer-group=email-service-test",
            "app.kafka.config-broadcast-group=email-service-test-config",
            "app.email.inbox.scheduler.enabled=false",
            "app.email.inbox.scheduler.poll-delay=10s",
            "app.email.runtime-config-cache.ttl=30m",
            "app.clients.instance-service.base-url=http://localhost:8001",
            "app.clients.instance-service.connect-timeout=2s",
            "app.clients.instance-service.read-timeout=5s"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(REQUIRED_PROPERTIES);

    @Nested
    class Binding {

        @Test
        void bindsJavaDefaultsAndNestedProperties() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();

                EmailInboxProperties inbox = context.getBean(EmailInboxProperties.class);
                EmailTransportProperties transport = context.getBean(EmailTransportProperties.class);

                assertThat(inbox.getProcessing().getLease()).isEqualTo(Duration.ofMinutes(5));
                assertThat(inbox.getProcessing().getBatchSize()).isEqualTo(25);
                assertThat(inbox.getRetry().getMaxAttempts()).isEqualTo(8);
                assertThat(inbox.getScheduler().isEnabled()).isFalse();
                assertThat(transport.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(5));
            });
        }

        @Test
        void environmentVariableOverridesPackagedValue() {
            contextRunner
                    .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                            new SystemEnvironmentPropertySource(
                                    "test-systemEnvironment",
                                    Map.of("APP_EMAIL_INBOX_PROCESSING_BATCHSIZE", "7"))))
                    .run(context -> assertThat(context.getBean(EmailInboxProperties.class)
                            .getProcessing()
                            .getBatchSize()).isEqualTo(7));
        }
    }

    @Nested
    class InvalidConfiguration {

        @Test
        void rejectsUnknownExclusivePrefixKey() {
            contextRunner
                    .withPropertyValues("app.email.transport.unused-timeout=1s")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void rejectsInvalidDuration() {
            contextRunner
                    .withPropertyValues("app.email.inbox.processing.lease=0s")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        void rejectsCrossFieldRetryRange() {
            contextRunner
                    .withPropertyValues(
                            "app.email.inbox.retry.initial-delay=2m",
                            "app.email.inbox.retry.max-delay=1m")
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({
            EmailInboxProperties.class,
            EmailKafkaProperties.class,
            EmailRuntimeConfigCacheProperties.class,
            EmailTransportProperties.class,
            InstanceServiceClientProperties.class
    })
    static class TestConfiguration {
    }
}
