package com.syncturtle.services.email.service.inbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.services.email.configuration.property.EmailInboxProperties;
import com.syncturtle.services.email.configuration.property.EmailInboxProperties.RetryProperties;
import com.syncturtle.services.email.exception.EmailInboxException;
import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.model.param.EmailEventInboxCreateParam;
import com.syncturtle.services.email.model.param.EmailEventInboxRetryFailureParam;
import com.syncturtle.services.email.repository.EmailEventInboxRepository;
import com.syncturtle.services.email.service.param.EmailDispatchParam;
import com.syncturtle.services.email.service.result.EmailInboxAcquireResult;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class EmailInboxStore {

    private static final int MAX_ERROR_LENGTH = 4_000;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };
    private static final TypeReference<Map<String, Object>> MODEL_MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final EmailEventInboxRepository repository;
    private final EmailInboxProperties properties;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailInboxAcquireResult insertFresh(EmailToSendEvent event) {
        Assert.notNull(event, "email event is required");

        EmailEventInboxCreateParam param = EmailEventInboxCreateParam.builder()
                .eventId(event.getEventId())
                .eventType(event.eventTypeName())
                .templateType(event.getTemplateType())
                .subject(event.getSubject())
                .recipientToJson(writeJson(event.getTo()))
                .templateModelJson(writeJson(event.getModel()))
                .build();

        EmailEventInbox inbox = EmailEventInbox.create(param, clock, properties.getProcessing().getLease());
        repository.saveAndFlush(inbox);
        return EmailInboxAcquireResult.acquired(inbox);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailInboxAcquireResult resolveExisting(String eventId) {
        Assert.hasText(eventId, "eventId is required");

        String normalizedEventId = eventId.trim();
        EmailEventInbox existing = repository.findByEventIdForUpdate(normalizedEventId)
                .orElseThrow(() -> EmailInboxException.rowNotFound(normalizedEventId));

        if (existing.isSent()) {
            return EmailInboxAcquireResult.alreadySent(existing);
        }

        if (existing.isPermanentFailure()) {
            return EmailInboxAcquireResult.permanentFailure(existing);
        }

        if (existing.hasActiveProcessingLease(clock)) {
            return EmailInboxAcquireResult.inProgress(existing);
        }

        if (existing.hasScheduledRetry(clock)) {
            return EmailInboxAcquireResult.retryScheduled(existing);
        }

        existing.claimForProcessing(clock, properties.getProcessing().getLease());
        repository.saveAndFlush(existing);
        return EmailInboxAcquireResult.acquired(existing);
    }

    @Transactional
    public List<EmailEventInbox> acquireDueRetries(int limit) {
        Assert.isTrue(limit >= 1, "limit must be at least 1");

        Instant now = Instant.now(clock);
        List<UUID> ids = new ArrayList<>(limit);
        List<UUID> dueRetryIds = repository.lockDueRetryIds(now, limit);

        ids.addAll(dueRetryIds);

        if (ids.size() < limit) {
            List<UUID> expiredProcessingIds = repository.lockExpiredProcessingIds(now, limit - ids.size());
            ids.addAll(expiredProcessingIds);
        }

        if (ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, EmailEventInbox> rowsById = repository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(EmailEventInbox::getId, row -> row, (left, right) -> left,
                        LinkedHashMap::new));

        Assert.state(rowsById.size() == ids.size(), "Every locked email inbox row must exist");

        List<EmailEventInbox> orderedRows = ids.stream()
                .map(id -> requireRow(rowsById, id))
                .sorted(Comparator.comparing(EmailEventInbox::getId))
                .toList();

        Duration processingLease = properties.getProcessing().getLease();

        for (EmailEventInbox row : orderedRows) {
            row.claimForProcessing(clock, processingLease);
        }

        return repository.saveAllAndFlush(orderedRows);
    }

    public EmailDispatchParam toDispatchParam(EmailEventInbox row) {
        Assert.notNull(row, "email inbox row is required");

        try {
            List<String> recipients = jsonMapper.readValue(row.getRecipientToJson(), STRING_LIST_TYPE);
            Map<String, Object> model = jsonMapper.readValue(row.getTemplateModelJson(), MODEL_MAP_TYPE);

            return EmailDispatchParam.builder()
                    .eventId(row.getEventId())
                    .templateType(row.getTemplateType())
                    .subject(row.getSubject())
                    .recipients(recipients)
                    .model(model)
                    .build();
        } catch (JacksonException exception) {
            throw EmailInboxException.payloadDeserializationFailed(row.getEventId(), exception);
        } catch (IllegalArgumentException exception) {
            // json valid while still violating emaildispatcherparam dev invariants
            throw EmailInboxException.payloadDeserializationFailed(row.getEventId(), exception);
        }
    }

    @Transactional
    public void markSent(String eventId) {
        EmailEventInbox inbox = findForUpdate(eventId);
        inbox.markSent(clock);
        repository.saveAndFlush(inbox);
    }

    @Transactional
    public void markRetryableFailure(String eventId, Exception exception) {
        Assert.notNull(exception, "exception is required");

        EmailEventInbox inbox = findForUpdate(eventId);
        RetryProperties retryProperties = properties.getRetry();
        EmailEventInboxRetryFailureParam param = new EmailEventInboxRetryFailureParam(retryProperties.getMaxAttempts(),
                computeRetryDelay(inbox.getAttemptCount()), summarizeException(exception));

        inbox.markRetryableFailure(param, clock);
        repository.saveAndFlush(inbox);
    }

    @Transactional
    public void markPermanentFailure(String eventId, Exception exception) {
        Assert.notNull(exception, "exception is required");

        EmailEventInbox inbox = findForUpdate(eventId);
        inbox.markPermanentFailure(summarizeException(exception), clock);
        repository.saveAndFlush(inbox);
    }

    private EmailEventInbox findForUpdate(String eventId) {
        Assert.hasText(eventId, "eventId is required");

        String normalizedEventId = eventId.trim();

        return repository.findByEventIdForUpdate(normalizedEventId)
                .orElseThrow(() -> EmailInboxException.rowNotFound(normalizedEventId));
    }

    private Duration computeRetryDelay(int currentAttemptCount) {
        Assert.isTrue(currentAttemptCount >= 1, "currentAttemptCount must be at least 1");

        RetryProperties retryProperties = properties.getRetry();

        Duration delay = retryProperties.getInitialDelay();
        Duration maximum = retryProperties.getMaxDelay();
        int doublings = currentAttemptCount - 1;

        for (int i = 0; i < doublings; i++) {
            if (delay.compareTo(maximum.dividedBy(2)) >= 0) {
                return maximum;
            }

            delay = delay.multipliedBy(2);
        }

        return delay.compareTo(maximum) > 0 ? maximum : delay;
    }

    private String summarizeException(Exception exception) {
        Assert.notNull(exception, "exception is required");

        Throwable root = exception;

        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        String message = root.getClass().getSimpleName();
        if (root.getMessage() != null && !root.getMessage().isBlank()) {
            message += ": " + root.getMessage();
        }

        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }

    private String writeJson(Object value) {
        Assert.notNull(value, "JSON value is required");

        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw EmailInboxException.payloadSerializationFailed(exception);
        }
    }

    private static EmailEventInbox requireRow(Map<UUID, EmailEventInbox> rowsById, UUID id) {
        EmailEventInbox row = rowsById.get(id);

        Assert.notNull(row, "Locked email inbox row is required: " + id);

        return row;
    }

}
