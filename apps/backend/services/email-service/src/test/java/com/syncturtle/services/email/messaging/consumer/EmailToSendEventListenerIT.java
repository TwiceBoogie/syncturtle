package com.syncturtle.services.email.messaging.consumer;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeEmailEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.email.EmailServiceApplication;
import com.syncturtle.services.email.service.EmailInboxService;
import com.syncturtle.services.email.support.kafka.EmailKafkaTopicsTestConfiguration;
import com.syncturtle.testing.annotations.IntegrationTest;
import com.syncturtle.testing.annotations.UseKafka;
import com.syncturtle.testing.annotations.UsePostgresDb;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Scope;

@IntegrationTest(classes = EmailServiceApplication.class, properties = {
        "app.email.topics.email-to-send=email.events.v1.email-listener-it",
        "app.email.topics.email-config-changed=instance-config.events.v1.email-listener-it",
        "app.kafka.enabled=true",
        "spring.kafka.template.observation-enabled=true",
        "spring.kafka.listener.observation-enabled=true",
        "management.tracing.sampling.probability=1.0"
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

    @Autowired
    OpenTelemetry openTelemetry;

    @MockitoBean
    EmailInboxService service;

    @Nested
    @DisplayName("onEmailToSend(EmailToSendEvent)")
    class OnEmailToSendTests {

        @Test
        @DisplayName("propagates W3C trace context and exposes consumer trace IDs to application logging")
        void propagatesW3cTraceContextAndExposesConsumerTraceIdsToApplicationLogging() throws Exception {
            // arrange
            EmailToSendEvent event = magicCodeEmailEvent();
            ArgumentCaptor<EmailToSendEvent> eventCaptor = ArgumentCaptor.forClass(EmailToSendEvent.class);
            AtomicReference<SpanContext> consumerSpanContext = new AtomicReference<>();
            AtomicReference<String> consumerMdcTraceId = new AtomicReference<>();
            AtomicReference<String> consumerMdcSpanId = new AtomicReference<>();
            CountDownLatch consumed = new CountDownLatch(1);
            // conditions
            doAnswer(invocation -> {
                consumerSpanContext.set(Span.current().getSpanContext());
                consumerMdcTraceId.set(MDC.get("traceId"));
                consumerMdcSpanId.set(MDC.get("spanId"));
                consumed.countDown();
                return null;
            }).when(service).receive(any(EmailToSendEvent.class));

            Span parent = openTelemetry.getTracer(getClass().getName())
                    .spanBuilder("email-kafka-propagation-test")
                    .startSpan();
            SpanContext parentContext = parent.getSpanContext();

            // act
            SendResult<String, Object> sendResult;
            try (Scope ignored = parent.makeCurrent()) {
                sendResult = kafkaTemplate.send(KafkaTopics.EMAIL_EVENTS_V1, EVENT_ID, event)
                        .get(5, TimeUnit.SECONDS);
            } finally {
                parent.end();
            }

            assertThat(consumed.await(10, TimeUnit.SECONDS)).isTrue();

            // assert + verify
            verify(service, timeout(10_000)).receive(eventCaptor.capture());
            EmailToSendEvent received = eventCaptor.getValue();
            assertThat(received.getEventId()).isEqualTo(EVENT_ID);

            byte[] traceparentBytes = sendResult.getProducerRecord()
                    .headers()
                    .lastHeader("traceparent")
                    .value();
            String traceparent = new String(traceparentBytes, StandardCharsets.UTF_8);
            String[] traceparentParts = traceparent.split("-");

            assertThat(traceparent)
                    .matches("^00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}$");
            assertThat(traceparentParts[1]).isEqualTo(parentContext.getTraceId());
            assertThat(traceparentParts[2]).isNotEqualTo(parentContext.getSpanId());

            SpanContext consumerContext = consumerSpanContext.get();
            assertThat(consumerContext.isValid()).isTrue();
            assertThat(consumerContext.getTraceId()).isEqualTo(parentContext.getTraceId());
            assertThat(consumerContext.getSpanId())
                    .isNotEqualTo(parentContext.getSpanId())
                    .isNotEqualTo(traceparentParts[2]);
            assertThat(consumerMdcTraceId.get()).isEqualTo(consumerContext.getTraceId());
            assertThat(consumerMdcSpanId.get()).isEqualTo(consumerContext.getSpanId());
        }

    }

}