package com.syncturtle.services.file.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.syncturtle.services.file.dto.response.FileAssetCleanupResponse;
import com.syncturtle.services.file.service.FileAssetCleanupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.file.cleanup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileAssetCleanupScheduler {

    private final FileAssetCleanupService cleanupService;

    @Scheduled(fixedDelayString = "${app.file.cleanup.fixed-delay-ms:60000}", initialDelayString = "${app.file.cleanup.initial-delay-ms:10000}")
    public void cleanupDueAssets() {
        try {
            FileAssetCleanupResponse response = cleanupService.cleanupDueAssets();

            if (response.getAssetsClaimed() > 0 || response.getIdempotencyRecordsDeleted() > 0) {
                log.info(
                        "File cleanup completed. assetsClaimed={} objectsDeleted={} objectDeletionFailures={} idempotencyRecordsDeleted={}",
                        response.getAssetsClaimed(),
                        response.getObjectsDeleted(),
                        response.getObjectDeletionFailures(),
                        response.getIdempotencyRecordsDeleted());
            }
        } catch (Exception exception) {
            log.error("File cleanup cycle failed. message={}", exception.getMessage(), exception);
        }
    }

}
