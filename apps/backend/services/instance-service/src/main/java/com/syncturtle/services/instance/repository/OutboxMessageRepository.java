package com.syncturtle.services.instance.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.syncturtle.services.instance.model.OutboxMessage;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, UUID> {

    @Query(value = """
            SELECT *
            FROM outbox_messages
            WHERE status IN ('NEW', 'FAILED')
                AND next_attempt_at <= :now
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessage> findDueForPublishing(Instant now, int limit);

    @Query(value = """
            SELECT *
            FROM outbox_messages
            WHERE status = 'PUBLISHING'
                AND locked_at < :lockedBefore
            ORDER BY locked_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxMessage> findExpiredPublishingLocks(Instant lockedBefore, int limit);

    List<OutboxMessage> findByIdIn(Collection<UUID> ids);

}
