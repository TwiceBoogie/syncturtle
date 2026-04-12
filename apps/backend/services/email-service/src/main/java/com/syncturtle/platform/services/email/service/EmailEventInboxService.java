package com.syncturtle.platform.services.email.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.core.enums.EmailTemplateType;
import com.syncturtle.common.core.events.EmailToSendEvent;
import com.syncturtle.platform.services.email.dto.EmailEnvelope;
import com.syncturtle.platform.services.email.enums.EmailEventInboxStatus;
import com.syncturtle.platform.services.email.models.EmailEventInbox;
import com.syncturtle.platform.services.email.repositories.EmailEventInboxRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailEventInboxService {

    private static final Duration PROCESSING_LEASE = Duration.ofMinutes(5);
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, Object>> MODEL_MAP_TYPE = new TypeReference<>() {
    };

    private final EmailEventInboxRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${app.email.retry.max-attempts:8}")
    private int maxAttempts;

    @Value("${app.email.retry.initial-delay-ms:30000}")
    private long initialRetryDelayMs;

    @Value("${app.email.retry.max-delay-ms:90000}")
    private long maxRetryDelayMs;

    @Transactional
    public AcquireResult tryAcquire(EmailToSendEvent event) {
        Instant now = Instant.now();

        try {
            EmailEventInbox fresh = new EmailEventInbox();
            populateSnapshot(fresh, event);
            fresh.setStatus(EmailEventInboxStatus.PROCESSING);
            fresh.setAttemptCount(1);
            fresh.setLockUntil(now.plus(PROCESSING_LEASE));
            fresh.setNextAttemptAt(null);
            fresh.setProcessedAt(null);
            fresh.setLastError(null);

            repository.saveAndFlush(fresh);
            return AcquireResult.acquired(fresh);
        } catch (DataIntegrityViolationException ignored) {
            // someone already inserted it
        }

        EmailEventInbox existing = repository.findByEventIdForUpdate(event.getEventId())
                .orElseThrow(
                        () -> new IllegalStateException("Inbox row disappeared for eventId=" + event.getEventId()));

        if (existing.getStatus() == EmailEventInboxStatus.SENT) {
            return AcquireResult.alreadySent(existing);
        }

        if (existing.getStatus() == EmailEventInboxStatus.FAILED_PERMANENT) {
            return AcquireResult.permanentFailure(existing);
        }

        if (existing.getStatus() == EmailEventInboxStatus.PROCESSING
                && existing.getLockUntil() != null
                && existing.getLockUntil().isAfter(now)) {
            return AcquireResult.inProgress(existing);
        }

        claimForProcessing(existing, now);
        repository.saveAndFlush(existing);

        return AcquireResult.acquired(existing);
    }

    @Transactional
    public List<EmailEventInbox> acquireDueRetries(int limit) {
        Instant now = Instant.now();

        List<UUID> ids = new ArrayList<>(limit);

        List<UUID> dueRetryIds = repository.lockDueRetryIds(now, limit);
        ids.addAll(dueRetryIds);

        if (ids.size() < limit) {
            ids.addAll(repository.lockExpiredProcessingIds(now, limit - ids.size()));
        }

        if (ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, EmailEventInbox> byId = repository.findAllById(ids).stream()
                .collect(Collectors.toMap(EmailEventInbox::getId, row -> row, (left, right) -> left,
                        LinkedHashMap::new));

        List<EmailEventInbox> orderedRows = ids.stream()
                .map(byId::get)
                .filter(row -> row != null)
                .sorted(Comparator.comparing(EmailEventInbox::getId))
                .toList();

        for (EmailEventInbox row : orderedRows) {
            claimForProcessing(row, now);
        }

        return repository.saveAllAndFlush(orderedRows);
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

    private void populateSnapshot(EmailEventInbox entity, EmailToSendEvent event) {
        entity.setEventId(event.getEventId());
        entity.setEventType(event.getClass().getSimpleName());
        entity.setCorrelationId(event.getCorrelationId());
        entity.setTemplateType(event.getTemplateType().name());
        entity.setSubject(event.getSubject());
        entity.setRecipientToJson(writeJson(event.getTo() == null ? List.of() : event.getTo()));
        entity.setTemplateModelJson(writeJson(event.getModel() == null ? Map.of() : event.getModel()));
    }

    private void claimForProcessing(EmailEventInbox entity, Instant now) {
        entity.setStatus(EmailEventInboxStatus.PROCESSING);
        entity.setAttemptCount(Math.max(1, entity.getAttemptCount() + 1));
        entity.setLockUntil(now.plus(PROCESSING_LEASE));
        entity.setNextAttemptAt(null);
        entity.setProcessedAt(null);
        entity.setLastError(null);
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize inbox payload", exception);
        }
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
