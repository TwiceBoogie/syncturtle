package com.syncturtle.services.file.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncturtle.services.file.model.IdempotencyRecord;

import jakarta.persistence.LockModeType;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT record
            FROM IdempotencyRecord record
            WHERE record.ownerUserId = :ownerUserId
                AND record.routeKey = :routeKey
                AND record.idempotencyKey = :idempotencyKey
            """)
    Optional<IdempotencyRecord> findByOperationKeyForUpdate(@Param("ownerUserId") UUID ownerUserId,
            @Param("routeKey") String routeKey, @Param("idempotencyKey") String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT record FROM IdempotencyRecord record WHERE record.id = :recordId")
    Optional<IdempotencyRecord> findByIdForUpdate(@Param("recordId") UUID recordId);

    @Modifying
    @Query(value = """
            DELETE FROM idempotency_record
            WHERE id IN (
                SELECT id
                FROM idempotency_record
                WHERE expires_at <= :now
                    AND (status <> 'PROCESSING'
                        OR processing_expires_at IS NULL
                        OR processing_expires_at <= :now)
                ORDER BY expires_at ASC, id ASC
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            """, nativeQuery = true)
    int deleteExpiredBatch(@Param("now") Instant now, @Param("limit") int limit);
}
