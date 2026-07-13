package com.syncturtle.services.file.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.file.model.IdempotencyRecord;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByOwnerUserIdAndRouteKeyAndIdempotencyKey(UUID ownerUserId, String routeKey,
            String idempotencyKey);
}
