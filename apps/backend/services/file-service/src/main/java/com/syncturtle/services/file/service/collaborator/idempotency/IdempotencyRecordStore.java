package com.syncturtle.services.file.service.collaborator.idempotency;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import com.syncturtle.services.file.configuration.property.FileUploadProperties;
import com.syncturtle.services.file.model.IdempotencyRecord;
import com.syncturtle.services.file.repository.IdempotencyRecordRepository;
import com.syncturtle.services.file.service.result.IdempotencyAcquireResult;

@Component
public class IdempotencyRecordStore {

    private final IdempotencyRecordRepository repository;
    private final FileUploadProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public IdempotencyRecordStore(
            IdempotencyRecordRepository repository,
            FileUploadProperties properties,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        Assert.notNull(repository, "idempotency record repository is required");
        Assert.notNull(properties, "file upload properties is required");
        Assert.notNull(transactionManager, "transactionManageer is required");
        Assert.notNull(clock, "clock is required");

        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public IdempotencyAcquireResult acquire(
            UUID ownerUserId,
            String routeKey,
            String idempotencyKey,
            String requestHash) {
        Assert.notNull(ownerUserId, "ownerUserId is required");
        Assert.hasText(routeKey, "routeKey is required");
        Assert.hasText(idempotencyKey, "idempotencyKey is required");
        Assert.hasText(requestHash, "requestHash is required");

        try {
            IdempotencyAcquireResult inserted = transactionTemplate.execute(status -> insertFresh(
                    ownerUserId,
                    routeKey,
                    idempotencyKey,
                    requestHash));
            return requireResult(inserted);
        } catch (DataIntegrityViolationException duplicate) {
            IdempotencyAcquireResult resolved = transactionTemplate.execute(status -> resolveExisting(
                    ownerUserId,
                    routeKey,
                    idempotencyKey,
                    requestHash));
            return requireResult(resolved);
        }
    }

    public IdempotencyRecord requireClaimedForUpdate(UUID recordId, UUID processingToken) {
        Assert.notNull(recordId, "recordId is required");
        Assert.notNull(processingToken, "processingToken is required");

        IdempotencyRecord record = repository.findByIdForUpdate(recordId)
                .orElseThrow(() -> new IllegalStateException("Claimed idempotency record was not found: " + recordId));

        Assert.state(record.processingTokenMatches(processingToken), "idempotency claim was superseded");

        return record;
    }

    public void markFailed(UUID recordId, UUID processingToken) {
        Assert.notNull(recordId, "recordId is required");
        Assert.notNull(processingToken, "processingToken is required");

        transactionTemplate.executeWithoutResult(status -> {
            IdempotencyRecord record = repository.findByIdForUpdate(recordId).orElse(null);
            if (record == null) {
                return;
            }

            record.failIfClaimed(processingToken, properties.getIdempotencyRetention(), clock);
        });
    }

    private IdempotencyAcquireResult insertFresh(
            UUID ownerUserId,
            String routeKey,
            String idempotencyKey,
            String requestHash) {
        IdempotencyRecord record = IdempotencyRecord.processing(
                idempotencyKey,
                ownerUserId,
                routeKey,
                requestHash,
                properties.getIdempotencyProcessingLease(),
                properties.getIdempotencyRetention(),
                clock);

        repository.saveAndFlush(record);
        return IdempotencyAcquireResult.acquired(record.getId(), record.getProcessingToken());
    }

    private IdempotencyAcquireResult resolveExisting(
            UUID ownerUserId,
            String routeKey,
            String idempotencyKey,
            String requestHash) {
        IdempotencyRecord existing = repository
                .findByOperationKeyForUpdate(ownerUserId, routeKey, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Conflicting idempotency record was not found"));

        if (existing.isExpired(clock)) {
            UUID token = existing.reclaim(
                    requestHash,
                    properties.getIdempotencyProcessingLease(),
                    properties.getIdempotencyRetention(),
                    clock);

            return IdempotencyAcquireResult.acquired(existing.getId(), token);
        }

        if (!existing.requestHashMatches(requestHash)) {
            return IdempotencyAcquireResult.conflict(existing.getId());
        }

        if (existing.isCompleted()) {
            return IdempotencyAcquireResult.replay(existing.getId(), existing.getResponseBody());
        }

        if (existing.hasActiveProcessingLease(clock)) {
            return IdempotencyAcquireResult.inProgress(existing.getId());
        }

        Duration processingLease = properties.getIdempotencyProcessingLease();
        Duration retention = properties.getIdempotencyRetention();
        UUID token = existing.reclaim(requestHash, processingLease, retention, clock);

        return IdempotencyAcquireResult.acquired(existing.getId(), token);
    }

    private static IdempotencyAcquireResult requireResult(IdempotencyAcquireResult result) {
        return Objects.requireNonNull(result, "idempotency acquire result is required");
    }

}
