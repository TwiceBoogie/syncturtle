package com.syncturtle.services.email.messaging.kafka.consumer;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeEmailEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.CloseOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.email.EmailServiceApplication;
import com.syncturtle.services.email.service.EmailInboxService;
import com.syncturtle.services.email.support.kafka.EmailKafkaTopicsTestConfiguration;
import com.syncturtle.testing.annotation.IntegrationTest;
import com.syncturtle.testing.annotation.UseKafka;
import com.syncturtle.testing.annotation.UsePostgresDb;
import com.syncturtle.testing.container.KafkaContainerSingleton;

@IntegrationTest(classes = EmailServiceApplication.class, properties = {
        "app.kafka.enabled=true",
        "app.kafka.error-handling.initial-interval=1ms",
        "app.kafka.error-handling.multiplier=1",
        "app.kafka.error-handling.max-elapsed-time=2ms",
        "app.kafka.error-handling.dlt-suffix=.DLT"
})
@UseKafka
@UsePostgresDb("email_service_dlt_it")
@Import(EmailKafkaTopicsTestConfiguration.class)
@DisplayName("Email kafka dead letter publishing")
class EmailKafkaDeadLetterPublishingIT {

    private static final String DLT_TOPIC = KafkaTopics.EMAIL_EVENTS_V1 + ".DLT";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private EmailInboxService service;

    private KafkaConsumer<String, String> dltConsumer;

    @AfterEach
    void closeConsumer() {
        if (dltConsumer != null) {
            dltConsumer.close(CloseOptions.timeout(Duration.ofSeconds(5)));
        }
    }

    @Nested
    @DisplayName("Recovery")
    class RecoveryTests {

        @Test
        @DisplayName("publishes exhausted email listener failures with original record diagnostics")
        void publishesExhaustedFailureToEmailDlt() throws Exception {
            // arrange
            EmailToSendEvent event = magicCodeEmailEvent();
            doThrow(new IllegalStateException("forced-email-listener-failure"))
                    .when(service).receive(any(EmailToSendEvent.class));
            dltConsumer = consumer();
            dltConsumer.subscribe(List.of(DLT_TOPIC));
            dltConsumer.poll(Duration.ofMillis(100));

            kafkaTemplate.send(KafkaTopics.EMAIL_EVENTS_V1, EVENT_ID, event).get(10, TimeUnit.SECONDS);

            ConsumerRecord<String, String> recovered = pollForRecord(dltConsumer, Duration.ofSeconds(15));

            assertThat(recovered).as("failed email event published to DLT").isNotNull();
            assertThat(recovered.topic()).isEqualTo(DLT_TOPIC);
            assertThat(recovered.partition()).isZero();
            assertThat(recovered.key()).isEqualTo(EVENT_ID);
            assertThat(recovered.value()).contains(EVENT_ID);
            assertThat(header(recovered, KafkaHeaders.DLT_ORIGINAL_TOPIC))
                    .isEqualTo(KafkaTopics.EMAIL_EVENTS_V1);
            assertThat(header(recovered, KafkaHeaders.DLT_EXCEPTION_MESSAGE))
                    .contains("forced-email-listener-failure");
        }

    }

    private static KafkaConsumer<String, String> consumer() {
        Map<String, Object> properties = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSingleton.bootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "email-dlt-assertion-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(properties);
    }

    private static ConsumerRecord<String, String> pollForRecord(KafkaConsumer<String, String> consumer,
            Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(250))) {
                return record;
            }
        }
        return null;
    }

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

}
