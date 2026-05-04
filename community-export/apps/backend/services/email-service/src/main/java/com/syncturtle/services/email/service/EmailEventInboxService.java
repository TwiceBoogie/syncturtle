package com.syncturtle.services.email.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.dto.EmailEnvelope;
import com.syncturtle.services.email.enums.EmailEventInboxStatus;
import com.syncturtle.services.email.models.EmailEventInbox;
import com.syncturtle.services.email.repositories.EmailEventInboxRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailEventInboxService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MODEL_MAP_TYPE = new TypeReference<>() {
    };

    private final EmailEventInboxAcquireTxService acquireTxService;
    private final EmailEventInboxRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${app.email.retry.max-attempts:8}")
    private int maxAttempts;

    @Value("${app.email.retry.initial-delay-ms:30000}")
    private long initialRetryDelayMs;

    @Value("${app.email.retry.max-delay-ms:90000}")
    private long maxRetryDelayMs;

    /**
     * orchestration endpoint
     * 
     * Important:
     * - first try a fresh insert in its own transaction
     * - on unique key collision, resolve existing row in a brand new transaction
     * 
     * <p>
     * This avoids trying to recover inside a transaction that postgresql has marked
     * as aborted.
     * </p>
     * 
     * @param event
     * @return
     */
    public AcquireResult tryAcquire(EmailToSendEvent event) {
        try {
            return acquireTxService.insertFresh(event);
        } catch (DataIntegrityViolationException exception) {
            return acquireTxService.resolveExisting(event.getEventId());
        }
    }

    public List<EmailEventInbox> acquireDueRetries(int limit) {
        return acquireTxService.acquireDueRetries(limit);
    }

    @Transactional(readOnly = true)
    public EmailEnvelope toEnvelope(EmailEventInbox row) {
        try {
            List<String> recipients = objectMapper.readValue(row.getRecipientToJson(), STRING_LIST_TYPE);
            Map<String, Object> model = objectMapper.readValue(row.getTemplateModelJson(), MODEL_MAP_TYPE);

            return EmailEnvelope.builder()
                    .templateType(EmailTemplateType.valueOf(row.getTemplateType()))
                    .subject(row.getSubject())
                    .to(recipients)
                    .model(model)
                    .correlationId(row.getCorrelationId())
                    .build();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to rebuild EmailEnvelope for eventId=" + row.getEventId(),
                    exception);
        }
    }

    @Transactional
    public void markSent(String eventId) {
        EmailEventInbox entity = repository.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalStateException("Inbox row not found for eventId=" + eventId));

        entity.setStatus(EmailEventInboxStatus.SENT);
        entity.setProcessedAt(Instant.now());
        entity.setLockUntil(null);
        entity.setNextAttemptAt(null);
        entity.setLastError(null);

        repository.saveAndFlush(entity);
    }

    @Transactional
    public void markRetryableFailure(String eventId, Exception exception) {
        EmailEventInbox entity = repository.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalStateException("Inbox row not found for eventId=" + eventId));

        Instant now = Instant.now();
        String errorMessage = summarizeException(exception);

        if (entity.getAttemptCount() >= maxAttempts) {
            entity.setStatus(EmailEventInboxStatus.FAILED_PERMANENT);
            entity.setProcessedAt(now);
            entity.setNextAttemptAt(null);
        } else {
            entity.setStatus(EmailEventInboxStatus.FAILED_RETRYABLE);
            entity.setProcessedAt(null);
            entity.setNextAttemptAt(now.plus(computeRetryDelay(entity.getAttemptCount())));
        }

        entity.setLockUntil(null);
        entity.setLastError(errorMessage);
        repository.saveAndFlush(entity);
    }

    @Transactional
    public void markPermanentFailure(String eventId, Exception exception) {
        EmailEventInbox entity = repository.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalStateException("Inbox row not found for eventId=" + eventId));

        entity.setStatus(EmailEventInboxStatus.FAILED_PERMANENT);
        entity.setProcessedAt(Instant.now());
        entity.setLockUntil(null);
        entity.setNextAttemptAt(null);
        entity.setLastError(summarizeException(exception));

        repository.saveAndFlush(entity);
    }

    private Duration computeRetryDelay(int currentAttemptCount) {
        long delayMs = initialRetryDelayMs;
        int doublings = Math.max(0, currentAttemptCount - 1);

        for (int i = 0; i < doublings; i++) {
            if (delayMs >= maxRetryDelayMs / 2) {
                delayMs = maxRetryDelayMs;
                break;
            }
            delayMs *= 2;
        }

        return Duration.ofMillis(Math.min(delayMs, maxRetryDelayMs));
    }

    private String summarizeException(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String message = root.getClass().getSimpleName()
                + (root.getMessage() == null || root.getMessage().isBlank() ? "" : ": " + root.getMessage());

        return truncate(message, 4000);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record AcquireResult(Decision decision, EmailEventInbox row) {
        public static AcquireResult acquired(EmailEventInbox row) {
            return new AcquireResult(Decision.ACQUIRED, row);
        }

        public static AcquireResult alreadySent(EmailEventInbox row) {
            return new AcquireResult(Decision.ALREADY_SENT, row);
        }

        public static AcquireResult inProgress(EmailEventInbox row) {
            return new AcquireResult(Decision.IN_PROGRESS, row);
        }

        public static AcquireResult retryScheduled(EmailEventInbox row) {
            return new AcquireResult(Decision.RETRY_SCHEDULED, row);
        }

        public static AcquireResult permanentFailure(EmailEventInbox row) {
            return new AcquireResult(Decision.PERMANENT_FAILURE, row);
        }
    }

    public enum Decision {
        ACQUIRED,
        ALREADY_SENT,
        IN_PROGRESS,
        RETRY_SCHEDULED,
        PERMANENT_FAILURE
    }

}
