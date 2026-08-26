package com.syncturtle.services.file.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncturtle.services.file.model.FileAssetLink;

import jakarta.persistence.LockModeType;

public interface FileAssetLinkRepository extends JpaRepository<FileAssetLink, UUID> {
    boolean existsByAssetIdAndDeletedAtIsNull(UUID assetId);

    long countByAssetIdAndDeletedAtIsNull(UUID assetId);

    Optional<FileAssetLink> findByTargetServiceAndTargetTypeAndTargetIdAndUsageTypeAndPrimaryTrue(String targetService,
            String targetType, UUID targetId, String usageType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT link
            FROM FileAssetLink link
            WHERE link.targetService = :targetService
                AND link.targetType = :targetType
                AND link.targetId = :targetId
                AND link.usageType = :usageType
                AND link.primary = true
            """)
    Optional<FileAssetLink> findPrimarySlotForUpdate(
            @Param("targetService") String targetService,
            @Param("targetType") String targetType,
            @Param("targetId") UUID targetId,
            @Param("usageType") String usageType);
}
