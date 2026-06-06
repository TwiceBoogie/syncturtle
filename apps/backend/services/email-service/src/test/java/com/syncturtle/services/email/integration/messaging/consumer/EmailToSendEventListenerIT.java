package com.syncturtle.services.email.integration.messaging.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.email.dto.EmailEnvelope;
import com.syncturtle.services.email.exception.EmailDispatchException;
import com.syncturtle.services.email.integration.support.EmailKafkaTopicsTestConfiguration;
import com.syncturtle.services.email.models.EmailEventInbox;
import com.syncturtle.services.email.repository.EmailEventInboxRepository;
import com.syncturtle.services.email.service.EmailDispatchService;
import com.syncturtle.services.email.type.EmailEventInboxStatus;
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

    @BeforeEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void onEmailToSend_whenFirstEvent_persistsSnapshot_dispatches_andMarksSent() {
        // arrange
        String eventId = UUID.randomUUID().toString();
        EmailToSendEvent event = event(eventId);

        doNothing().when(emailDispatchService).send(any(EmailEnvelope.class));
        // act
        publish(event);
        // assert dispatch arguments
        ArgumentCaptor<EmailEnvelope> envelopeCaptor = ArgumentCaptor.forClass(EmailEnvelope.class);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            verify(emailDispatchService, times(1)).send(envelopeCaptor.capture());
        });

        EmailEnvelope sentEnvelope = envelopeCaptor.getValue();
        assertThat(sentEnvelope.getTemplateType()).isEqualTo(EmailTemplateType.MAGIC_LINK);
        assertThat(sentEnvelope.getSubject()).isEqualTo("Your magic link");
        assertThat(sentEnvelope.getTo()).containsExactly("lunasnow@marvel.com");
        assertThat(sentEnvelope.getCorrelationId()).isEqualTo("corr-123");
        assertThat(sentEnvelope.getModel()).containsEntry("firstName", "Luna");
        assertThat(sentEnvelope.getModel()).containsEntry("magicLink", "https://app.syncturtle.com/magic?token=abc");

        // assert persisted row
        EmailEventInbox row = awaitRowWithStatus(eventId, EmailEventInboxStatus.SENT);
        assertThat(repository.count()).isEqualTo(1);

        assertThat(row.getEventId()).isEqualTo(eventId);
        assertThat(row.getEventType()).isEqualTo("EmailToSendEvent");
        assertThat(row.getCorrelationId()).isEqualTo("corr-123");
        assertThat(row.getTemplateType()).isEqualTo("MAGIC_LINK");
        assertThat(row.getSubject()).isEqualTo("Your magic link");
        assertThat(row.getRecipientToJson()).contains("lunasnow@marvel.com");
        assertThat(row.getTemplateModelJson()).contains("firstName");
        assertThat(row.getTemplateModelJson()).contains("magicLink");

        assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.SENT);
        assertThat(row.getAttemptCount()).isEqualTo(1);
        assertThat(row.getProcessedAt()).isNotNull();
        assertThat(row.getNextAttemptAt()).isNull();
        assertThat(row.getLockUntil()).isNull();
        assertThat(row.getLastError()).isNull();
    }

    @Test
    void onEmailToSend_whenDuplicateArrivesWhileFirstIsStillProcessing_doesNotDispatchTwice()
            throws Exception {
        // arrange
        String eventId = UUID.randomUUID().toString();
        EmailToSendEvent event = event(eventId);
        CountDownLatch firstSendEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstSend = new CountDownLatch(1);
        AtomicInteger sendCalls = new AtomicInteger();

        doAnswer(invocation -> {
            sendCalls.incrementAndGet();
            firstSendEntered.countDown();
            boolean released = releaseFirstSend.await(10, TimeUnit.SECONDS);
            assertThat(released).isTrue();

            return null;
        }).when(emailDispatchService).send(any(EmailEnvelope.class));
        // act
        publish(event);
        assertThat(firstSendEntered.await(10, TimeUnit.SECONDS)).isTrue();

        // row should be actively processing
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            EmailEventInbox row = findRowOrThrow(eventId);
            assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.PROCESSING);
            assertThat(row.getAttemptCount()).isEqualTo(1);
            assertThat(row.getLockUntil()).isNotNull();
            assertThat(row.getLockUntil()).isAfter(Instant.now());
        });

        // send dupe while first is still in progress
        publish(event);

        // assert: no second dispatch while active processing lease exists
        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> {
                    assertThat(sendCalls.get()).isEqualTo(1);
                    verify(emailDispatchService, times(1)).send(any(EmailEnvelope.class));
                    EmailEventInbox row = findRowOrThrow(eventId);

                    assertThat(repository.count()).isEqualTo(1);
                    assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.PROCESSING);
                    assertThat(row.getAttemptCount()).isEqualTo(1);
                });

        // release first dispatch and allow pipeline to finish
        releaseFirstSend.countDown();

        EmailEventInbox finalRow = awaitRowWithStatus(eventId,
                EmailEventInboxStatus.SENT);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(finalRow.getAttemptCount()).isEqualTo(1);
        assertThat(finalRow.getProcessedAt()).isNotNull();
        assertThat(finalRow.getLockUntil()).isNull();
        assertThat(finalRow.getNextAttemptAt()).isNull();
        assertThat(finalRow.getLastError()).isNull();
        // verify
        verify(emailDispatchService, times(1)).send(any(EmailEnvelope.class));
    }

    @Test
    void onEmailToSend_whenSameEventPublishedAgainAfterSent_doesNotCreateNewRowOrDispatchAgain() {
        // arrange
        String eventId = UUID.randomUUID().toString();
        EmailToSendEvent event = event(eventId);
        doNothing().when(emailDispatchService).send(any(EmailEnvelope.class));

        // first delivery
        publish(event);
        EmailEventInbox firstRow = awaitRowWithStatus(eventId,
                EmailEventInboxStatus.SENT);
        assertThat(firstRow.getAttemptCount()).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);

        verify(emailDispatchService, times(1)).send(any(EmailEnvelope.class));

        // clear mock history so it can assert dupe behavior cleanly
        clearInvocations(emailDispatchService);

        // act: duplicate event after already SENT
        publish(event);

        // assert: still only one row, still SENT, no redispatch
        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> {
                    assertThat(repository.count()).isEqualTo(1);
                    EmailEventInbox row = findRowOrThrow(eventId);

                    assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.SENT);
                    assertThat(row.getAttemptCount()).isEqualTo(1);
                    assertThat(row.getProcessedAt()).isNotNull();

                    verifyNoInteractions(emailDispatchService);
                });
    }

    @Test
    void onEmailSend_whenDispatchFailsRetryably_marksFailedRetryableAndSchedulesNextAttempt() {
        // arrange
        String eventId = UUID.randomUUID().toString();
        EmailToSendEvent event = event(eventId);
        RuntimeException thrown = new IllegalStateException("smtp unavailable");

        doThrow(EmailDispatchException.sendFailed(thrown))
                .when(emailDispatchService)
                .send(any(EmailEnvelope.class));
        // act
        publish(event);
        // assert
        EmailEventInbox row = awaitRowWithStatus(eventId,
                EmailEventInboxStatus.FAILED_RETRYABLE);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(row.getAttemptCount()).isEqualTo(1);
        assertThat(row.getProcessedAt()).isNull();
        assertThat(row.getNextAttemptAt()).isNotNull();
        assertThat(row.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(row.getLockUntil()).isNull();
        assertThat(row.getLastError()).contains("smtp unavailable");
        // verify
        verify(emailDispatchService, times(1)).send(any(EmailEnvelope.class));
    }

    @Test
    void onEmailToSend_whenDispatchFailsPermanently_marksFailedPermanent() {
        // arrange
        String eventId = UUID.randomUUID().toString();
        EmailToSendEvent event = event(eventId);
        RuntimeException thrown = new IllegalArgumentException("template missing variable");

        doThrow(EmailDispatchException.templateFailed(thrown))
                .when(emailDispatchService)
                .send(any(EmailEnvelope.class));
        // act
        publish(event);
        // assert
        EmailEventInbox row = awaitRowWithStatus(eventId,
                EmailEventInboxStatus.FAILED_PERMANENT);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(row.getAttemptCount()).isEqualTo(1);
        assertThat(row.getProcessedAt()).isNotNull();
        assertThat(row.getNextAttemptAt()).isNull();
        assertThat(row.getLockUntil()).isNull();
        assertThat(row.getLastError()).contains("template missing variable");
        // verify
        verify(emailDispatchService, times(1)).send(any(EmailEnvelope.class));
    }

    @Test
    void onEmailToSend_whenExistingProcessingLeaseExpired_requiresRowAndProcessingAgain() {
        // arrange
        String eventId = UUID.randomUUID().toString();
        saveInboxRow(
                eventId,
                EmailEventInboxStatus.PROCESSING,
                1,
                Instant.now().minusSeconds(30),
                null,
                null,
                null);
        doNothing().when(emailDispatchService).send(any(EmailEnvelope.class));
        // act
        publish(event(eventId));
        // assert
        EmailEventInbox row = awaitRowWithStatus(eventId,
                EmailEventInboxStatus.SENT);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(row.getAttemptCount()).isEqualTo(2);
        assertThat(row.getProcessedAt()).isNotNull();
        assertThat(row.getNextAttemptAt()).isNull();
        assertThat(row.getLockUntil()).isNull();
        assertThat(row.getLastError()).isNull();
        // verify
        verify(emailDispatchService, times(1)).send(any(EmailEnvelope.class));
    }

    @Test
    void onEmailToSend_whenExistingRowIsPermanentFailure_skipsDuplicateWithoutDispatch() {
        // arrange
        String eventId = UUID.randomUUID().toString();
        saveInboxRow(
                eventId,
                EmailEventInboxStatus.FAILED_PERMANENT,
                3,
                null,
                null,
                Instant.now().minusSeconds(60),
                "IllegalArgumentException: template missing variable");
        // act
        publish(event(eventId));
        // assert
        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> {
                    assertThat(repository.count()).isEqualTo(1);

                    EmailEventInbox row = findRowOrThrow(eventId);
                    assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.FAILED_PERMANENT);
                    assertThat(row.getAttemptCount()).isEqualTo(3);
                    assertThat(row.getLastError()).contains("template missing variable");
                    // verify
                    verifyNoInteractions(emailDispatchService);
                });
    }

    @Disabled("Enable after EmailEventInboxService.tryAcquire() returns RETRY_SCHEDULED for FAILED_RETRYABLE rows whose nextAttemptAt is still in the future.")
    @Test
    void onEmailToSend_whenExistingRowIsRetryableButNotYetDue_skipsDuplicateUntilDue() {
        // arrange
        String eventId = UUID.randomUUID().toString();
        saveInboxRow(
                eventId,
                EmailEventInboxStatus.FAILED_RETRYABLE,
                2,
                null,
                Instant.now().plusSeconds(300),
                null,
                "IllegalStateException: smtp unavailable");
        // act
        publish(event(eventId));
        // assert
        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(4))
                .untilAsserted(() -> {
                    assertThat(repository.count()).isEqualTo(2);

                    EmailEventInbox row = findRowOrThrow(eventId);
                    assertThat(row.getStatus()).isEqualTo(EmailEventInboxStatus.FAILED_RETRYABLE);
                    assertThat(row.getAttemptCount()).isEqualTo(2);
                    assertThat(row.getNextAttemptAt()).isAfter(Instant.now());

                    verifyNoInteractions(emailDispatchService);
                });
    }

    private void publish(EmailToSendEvent event) {
        kafkaTemplate.send(KafkaTopics.EMAIL_EVENTS_V1, event.getEventId(), event);
        kafkaTemplate.flush();
    }

    private EmailToSendEvent event(String eventId) {
        return EmailToSendEvent.builder()
                .eventId(eventId)
                .correlationId("corr-123")
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject("Your magic link")
                .to(List.of("lunasnow@marvel.com"))
                .model(Map.of(
                        "firstName", "Luna",
                        "magicLink", "https://app.syncturtle.com/magic?token=abc"))
                .build();
    }

    @SuppressWarnings("unused")
    private EmailEventInbox saveInboxRow(
            String eventId,
            EmailEventInboxStatus status,
            int attemptCount,
            Instant lockUntil,
            Instant nextAttemptAt,
            Instant processedAt,
            String lastError) {
        EmailEventInbox row = new EmailEventInbox();
        row.setEventId(eventId);
        row.setEventType("EmailToSendEvent");
        row.setCorrelationId("corr-123");
        row.setTemplateType("MAGIC_LINK");
        row.setSubject("Your magic link");
        row.setRecipientToJson("[\"lunasnow@marvel.com\"]");
        row.setTemplateModelJson("""
                {"firstName":"Luna","magicLink":"https://app.syncturtle.com/magic?token=abc"}
                """);
        row.setStatus(status);
        row.setAttemptCount(attemptCount);
        row.setLockUntil(lockUntil);
        row.setNextAttemptAt(nextAttemptAt);
        row.setProcessedAt(processedAt);
        row.setLastError(lastError);

        return repository.saveAndFlush(row);
    }

    private EmailEventInbox awaitRowWithStatus(String eventId, EmailEventInboxStatus status) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            EmailEventInbox row = findRowOrThrow(eventId);
            assertThat(row.getStatus()).isEqualTo(status);
        });

        return findRowOrThrow(eventId);
    }

    private EmailEventInbox findRowOrThrow(String eventId) {
        return findRow(eventId).orElseThrow(() -> new AssertionError("Row not found for eventId=" + eventId));
    }

    private Optional<EmailEventInbox> findRow(String eventId) {
        return repository.findAll()
                .stream()
                .filter(row -> eventId.equals(row.getEventId()))
                .findFirst();
    }

}
