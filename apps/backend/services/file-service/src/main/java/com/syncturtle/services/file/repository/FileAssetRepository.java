package com.syncturtle.services.file.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncturtle.services.file.model.FileAsset;

import jakarta.persistence.LockModeType;

public interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {
    Optional<FileAsset> findByIdAndDeletedFlagFalse(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT asset FROM FileAsset asset WHERE asset.id = :assetId")
    Optional<FileAsset> findByIdForUpdate(@Param("assetId") UUID assetId);

    @Query(value = """
            SELECT id
            FROM file_assets
            WHERE storage_deleted_at IS NULL
                AND upload_expires_at IS NOT NULL
                AND upload_expires_at <= :now
                AND (
                    status = 'PENDING_UPLOAD'
                    OR is_deleted = true
                    OR (
                        status = 'UPLOADED'
                        AND uploaded_at <= :unlinkedBefore
                        AND NOT EXISTS (
                            SELECT 1
                            FROM file_asset_links link
                            WHERE link.asset_id = file_assets.id
                                AND link.deleted_at IS NULL
                        )
                    )
                )
                AND (storage_cleanup_claimed_until IS NULL OR storage_cleanup_claimed_until <= :now)
            ORDER BY upload_expires_at ASC, id ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> lockStorageCleanupCandidateIds(@Param("now") Instant now,
            @Param("unlinkedBefore") Instant unlinkedBefore,
            @Param("limit") int limit);
}
