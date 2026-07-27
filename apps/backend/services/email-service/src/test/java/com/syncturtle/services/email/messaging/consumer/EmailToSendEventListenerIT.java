package com.syncturtle.services.email.messaging.consumer;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeEmailEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.email.EmailServiceApplication;
import com.syncturtle.services.email.service.EmailInboxService;
import com.syncturtle.services.email.support.kafka.EmailKafkaTopicsTestConfiguration;
import com.syncturtle.testing.annotations.IntegrationTest;
import com.syncturtle.testing.annotations.UseKafka;
import com.syncturtle.testing.annotations.UsePostgresDb;

@IntegrationTest(classes = EmailServiceApplication.class, properties = {
        "app.email.topics.email-to-send=email.events.v1.email-listener-it",
        "app.email.topics.email-config-changed=instance-config.events.v1.email-listener-it"
})
@UseKafka
@UsePostgresDb("email_service_email_listener_it")
@Import(EmailKafkaTopicsTestConfiguration.class)
@DisplayName("EmailToSendEventListener")
class EmailToSendEventListenerIT {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    Environment environment;

    @MockitoBean
    EmailInboxService service;

    @Nested
    @DisplayName("onEmailToSend(EmailToSendEvent)")
    class OnEmailToSendTests {

        @Test
        @DisplayName("deserializes event and delegates it to public service")
        void deserializesEventAndDelegatesItToPublicService() throws Exception {
            // arrange
            EmailToSendEvent event = magicCodeEmailEvent();
            ArgumentCaptor<EmailToSendEvent> eventCaptor = ArgumentCaptor.forClass(EmailToSendEvent.class);
            // conditions
            // act
            kafkaTemplate.send(KafkaTopics.EMAIL_EVENTS_V1, EVENT_ID, event).get(5, TimeUnit.SECONDS);
            // assert + verify
            verify(service, timeout(10_000)).receive(eventCaptor.capture());
            EmailToSendEvent received = eventCaptor.getValue();
            assertThat(received.getEventId()).isEqualTo(EVENT_ID);
        }

    }

}