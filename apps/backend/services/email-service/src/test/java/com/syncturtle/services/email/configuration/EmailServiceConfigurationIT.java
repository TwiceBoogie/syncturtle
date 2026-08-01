package com.syncturtle.services.email.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.syncturtle.services.email.configuration.property.EmailInboxProperties;
import com.syncturtle.services.email.configuration.property.EmailKafkaProperties;
import com.syncturtle.services.email.configuration.property.EmailRuntimeConfigCacheProperties;
import com.syncturtle.services.email.configuration.property.EmailTransportProperties;
import com.syncturtle.services.email.configuration.property.InstanceServiceClientProperties;

@DisplayName("Email service configuration binding")
class EmailServiceConfigurationIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EmailServicePropertiesConfiguration.class)
            .withPropertyValues(
                    "app.kafka.enabled=true",
                    "app.kafka.consumer-group=email-workers",
                    "app.kafka.config-broadcast-group=email-config-broadcast",
                    "app.email.inbox.processing.lease=2m",
                    "app.email.inbox.processing.batch-size=17",
                    "app.email.inbox.retry.max-attempts=4",
                    "app.email.inbox.retry.initial-delay=3s",
                    "app.email.inbox.retry.max-delay=12s",
                    "app.email.inbox.scheduler.enabled=false",
                    "app.email.inbox.scheduler.poll-delay=9s",
                    "app.email.transport.connection-timeout=1s",
                    "app.email.transport.read-timeout=2s",
                    "app.email.transport.write-timeout=3s",
                    "app.email.runtime-config-cache.ttl=90s",
                    "app.clients.instance-service.base-url=https://instance.internal/",
                    "app.clients.instance-service.connect-timeout=2s",
                    "app.clients.instance-service.read-timeout=5s");

    @Nested
    @DisplayName("valid configuration")
    class ValidConfiguration {

        @Test
        @DisplayName("binds every email-service property family with strict service prefixes")
        void bindsAllPropertyFamilies() {
            contextRunner.run(context -> {
                assertThat(context).hasNotFailed();

                EmailKafkaProperties kafka = context.getBean(EmailKafkaProperties.class);
                assertThat(kafka.isEnabled()).isTrue();
                assertThat(kafka.getConsumerGroup()).isEqualTo("email-workers");
                assertThat(kafka.getConfigBroadcastGroup()).isEqualTo("email-config-broadcast");

                EmailInboxProperties inbox = context.getBean(EmailInboxProperties.class);
                assertThat(inbox.getProcessing().getLease()).isEqualTo(Duration.ofMinutes(2));
                assertThat(inbox.getProcessing().getBatchSize()).isEqualTo(17);
                assertThat(inbox.getRetry().getMaxAttempts()).isEqualTo(4);
                assertThat(inbox.getRetry().getInitialDelay()).isEqualTo(Duration.ofSeconds(3));
                assertThat(inbox.getRetry().getMaxDelay()).isEqualTo(Duration.ofSeconds(12));
                assertThat(inbox.getScheduler().isEnabled()).isFalse();
                assertThat(inbox.getScheduler().getPollDelay()).isEqualTo(Duration.ofSeconds(9));

                EmailTransportProperties transport = context.getBean(EmailTransportProperties.class);
                assertThat(transport.getConnectionTimeoutMillis()).isEqualTo(1_000);
                assertThat(transport.getReadTimeoutMillis()).isEqualTo(2_000);
                assertThat(transport.getWriteTimeoutMillis()).isEqualTo(3_000);

                assertThat(context.getBean(EmailRuntimeConfigCacheProperties.class).getTtl())
                        .isEqualTo(Duration.ofSeconds(90));

                InstanceServiceClientProperties client = context.getBean(InstanceServiceClientProperties.class);
                assertThat(client.getBaseUrl()).isEqualTo(URI.create("https://instance.internal"));
                assertThat(client.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
                assertThat(client.getReadTimeout()).isEqualTo(Duration.ofSeconds(5));
            });
        }

    }

    @Nested
    @DisplayName("invalid configuration")
    class InvalidConfiguration {

        @Test
        @DisplayName("fails when a strict email property contains an unknown key")
        void rejectsUnknownEmailProperty() {
            contextRunner.withPropertyValues("app.email.transport.legacy-timeout=4s")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("fails when retry delay bounds are inverted")
        void rejectsInvertedRetryDelayBounds() {
            contextRunner.withPropertyValues(
                    "app.email.inbox.retry.initial-delay=30s",
                    "app.email.inbox.retry.max-delay=10s")
                    .run(context -> assertThat(context).hasFailed());
        }

        @Test
        @DisplayName("fails when the instance-service URL contains an endpoint path")
        void rejectsInstanceServiceEndpointPath() {
            contextRunner.withPropertyValues(
                    "app.clients.instance-service.base-url=https://instance.internal/internal/v1")
                    .run(context -> assertThat(context).hasFailed());
        }

    }

}
