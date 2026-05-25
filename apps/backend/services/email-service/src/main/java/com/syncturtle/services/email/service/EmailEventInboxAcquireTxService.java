package com.syncturtle.services.email.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.services.email.enums.EmailEventInboxStatus;
import com.syncturtle.services.email.exceptions.EmailInboxException;
import com.syncturtle.services.email.models.EmailEventInbox;
import com.syncturtle.services.email.repositories.EmailEventInboxRepository;
import com.syncturtle.services.email.service.EmailEventInboxService.AcquireResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailEventInboxAcquireTxService {

    private static final Duration PROCESSING_LEASE = Duration.ofMinutes(5);

    private final EmailEventInboxRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Fresh insert attempt in its own transaction.
     * 
     * <p>
     * If this hits the unique constraint, postgresql will abort THIS transaction,
     * which is ok because the caller will recover by opening up a new one.
     * <p>
     * 
     * @param event The kafka event
     * @return The result that will determine email publishing
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AcquireResult insertFresh(EmailToSendEvent event) {
        Instant now = Instant.now();

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
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AcquireResult resolveExisting(String eventId) {
        Instant now = Instant.now();

        EmailEventInbox existing = repository.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> EmailInboxException.rowNotFound(eventId));

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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw EmailInboxException.payloadSerializationFailed(exception);
        }
    }

}
