package com.syncturtle.services.file.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import com.syncturtle.services.file.configuration.property.FileCleanupProperties;
import com.syncturtle.services.file.dto.response.FileAssetCleanupResponse;
import com.syncturtle.services.file.model.FileAsset;
import com.syncturtle.services.file.repository.FileAssetRepository;
import com.syncturtle.services.file.repository.IdempotencyRecordRepository;
import com.syncturtle.services.file.service.FileAssetCleanupService;
import com.syncturtle.services.file.service.collaborator.storage.ObjectStorageGateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAssetCleanupServiceImpl implements FileAssetCleanupService {

    private final FileAssetRepository assetRepository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final ObjectStorageGateway storage;
    private final FileCleanupProperties properties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Override
    public FileAssetCleanupResponse cleanupDueAssets() {
        int idempotencyRecordsDeleted = deleteExpiredIdempotencyRecords();
        List<StorageCleanupClaim> claims = claimDueAssets();
        int objectsDeleted = 0;
        int objectDeletionFailures = 0;

        for (StorageCleanupClaim claim : claims) {
            try {
                storage.deleteObjectVersions(claim.objectKey);
                markStorageDeleted(claim);
                objectsDeleted++;
            } catch (Exception exception) {
                objectDeletionFailures++;
                log.warn(
                        "Storage object cleanup failed; the claim will become retryable after its lease. assetId={} message={}",
                        claim.assetId,
                        exception.getMessage(),
                        exception);
            }
        }

        return FileAssetCleanupResponse.builder()
                .assetsClaimed(claims.size())
                .objectsDeleted(objectsDeleted)
                .objectDeletionFailures(objectDeletionFailures)
                .idempotencyRecordsDeleted(idempotencyRecordsDeleted)
                .build();
    }

    private int deleteExpiredIdempotencyRecords() {
        Integer deleted = transactionTemplate.execute(
                status -> idempotencyRepository.deleteExpiredBatch(Instant.now(clock), properties.getBatchSize()));
        return Objects.requireNonNullElse(deleted, 0);
    }

    private List<StorageCleanupClaim> claimDueAssets() {
        List<StorageCleanupClaim> claims = transactionTemplate.execute(status -> {
            Instant now = Instant.now(clock);
            Instant unlinkedBefore = now.minus(properties.getUnlinkedAssetRetention());
            List<UUID> ids = assetRepository.lockStorageCleanupCandidateIds(
                    now,
                    unlinkedBefore,
                    properties.getBatchSize());

            if (ids.isEmpty()) {
                return List.of();
            }

            Map<UUID, FileAsset> assetsById = new LinkedHashMap<>();
            for (FileAsset asset : assetRepository.findAllById(ids)) {
                assetsById.put(asset.getId(), asset);
            }

            Assert.state(assetsById.size() == ids.size(), "Every locked file asset must exist");

            List<UUID> orderedIds = new ArrayList<>(ids);
            orderedIds.sort(Comparator.naturalOrder());

            List<StorageCleanupClaim> acquired = new ArrayList<>(orderedIds.size());
            for (UUID id : orderedIds) {
                FileAsset asset = Objects.requireNonNull(assetsById.get(id), "locked file asset is required");

                if (asset.isPendingUpload()) {
                    asset.expirePendingUpload(clock);
                } else if (asset.isUploaded()) {
                    asset.markDeleted(clock);
                }

                UUID claimToken = asset.claimStorageCleanup(clock, properties.getClaimLease());
                acquired.add(new StorageCleanupClaim(asset.getId(), asset.getObjectKey(), claimToken));
            }

            assetRepository.flush();
            return List.copyOf(acquired);
        });

        return Objects.requireNonNullElse(claims, List.of());
    }

    private void markStorageDeleted(StorageCleanupClaim claim) {
        transactionTemplate.executeWithoutResult(status -> {
            FileAsset asset = assetRepository.findByIdForUpdate(claim.assetId)
                    .orElseThrow(() -> new IllegalStateException("Claimed file asset was not found: " + claim.assetId));

            if (!asset.storageCleanupClaimMatches(claim.claimToken)) {
                log.info("Storage cleanup completion ignored because the claim was superseded. assetId={}",
                        claim.assetId);

                return;
            }

            asset.markStorageDeleted(claim.claimToken, clock);
        });
    }

    private static class StorageCleanupClaim {
        private final UUID assetId;
        private final String objectKey;
        private final UUID claimToken;

        public StorageCleanupClaim(UUID assetId, String objectKey, UUID claimToken) {
            Assert.notNull(assetId, "assetId is required");
            Assert.hasText(objectKey, "objectKey is required");
            Assert.notNull(claimToken, "claimToken is required");

            this.assetId = assetId;
            this.objectKey = objectKey;
            this.claimToken = claimToken;
        }
    }

}
