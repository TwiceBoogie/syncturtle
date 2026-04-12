package com.syncturtle.platform.services.email.integration.messaging.consumer;

import static org.awaitility.Awaitility.await;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.syncturtle.common.core.constants.KafkaTopicConstants;
import com.syncturtle.common.core.enums.EmailTemplateType;
import com.syncturtle.common.core.events.EmailToSendEvent;
import com.syncturtle.platform.services.email.integration.support.EmailKafkaTopicsTestConfiguration;
import com.syncturtle.platform.services.email.repositories.EmailEventInboxRepository;
import com.syncturtle.platform.services.email.service.EmailDispatchService;
import com.syncturtle.testing.annotations.IntegrationTest;
import com.syncturtle.testing.annotations.UseKafka;
import com.syncturtle.testing.annotations.UsePostgresDb;

@UseKafka
@IntegrationTest
@UsePostgresDb("email_service_it")
@Import(EmailKafkaTopicsTestConfiguration.class)
class EmailToSendEventListenerIT {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    EmailEventInboxRepository repository;

    @MockitoBean
    EmailDispatchService emailDispatchService;

    @Test
    void onEmailToSend_whenFirstEvent_persistsAndMarksSent() {
        String eventId = UUID.randomUUID().toString();

        EmailToSendEvent event = EmailToSendEvent.builder()
                .eventId(eventId)
                .correlationId("corr-123")
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject("Your magic link")
                .to(List.of("user@example.com"))
                .model(Map.of(
                        "firstName", "Luna",
                        "magicLink", "https://app.syncturtle.com/magic?token=abc"))
                .build();

        kafkaTemplate.send(KafkaTopicConstants.EMAIL_EVENTS_V1, eventId, event);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(emailDispatchService, atLeastOnce()).send(any());
        });
    }

}
