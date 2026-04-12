package com.syncturtle.platform.services.email.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncturtle.platform.services.email.models.EmailEventInbox;

import jakarta.persistence.LockModeType;

public interface EmailEventInboxRepository extends JpaRepository<EmailEventInbox, UUID> {
    Optional<EmailEventInbox> findByEventId(String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM EmailEventInbox e
            WHERE e.eventId = :eventId
            """)
    Optional<EmailEventInbox> findByEventIdForUpdate(@Param("eventId") String eventId);

    @Query(value = """
            SELECT id
            FROM email_event_inbox
            WHERE status = 'FAILED_RETRYABLE'
                AND next_attempt_at IS NOT NULL
                AND next_attempt_at <= :now
            ORDER BY next_attempt_at ASC, id ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> lockDueRetryIds(@Param("now") Instant now, @Param("limit") int limit);

    @Query(value = """
            SELECT id
            FROM email_event_inbox
            WHERE status = 'PROCESSING'
                AND lock_until IS NOT NULL
                AND lock_until <= :now
            ORDER BY lock_until ASC, id ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> lockExpiredProcessingIds(@Param("now") Instant now, @Param("limit") int limit);
}
