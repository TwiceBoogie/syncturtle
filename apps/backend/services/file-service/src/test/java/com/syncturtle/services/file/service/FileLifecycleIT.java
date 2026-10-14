package com.syncturtle.services.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.unit.DataSize;

import com.syncturtle.common.contracts.file.FileAssetPurpose;
import com.syncturtle.services.file.configuration.property.FileCleanupProperties;
import com.syncturtle.services.file.configuration.property.FileUploadProperties;
import com.syncturtle.services.file.dto.response.FileAssetCleanupResponse;
import com.syncturtle.services.file.model.FileAsset;
import com.syncturtle.services.file.model.IdempotencyRecord;
import com.syncturtle.services.file.model.param.FileAssetCreateParam;
import com.syncturtle.services.file.repository.FileAssetRepository;
import com.syncturtle.services.file.repository.IdempotencyRecordRepository;
import com.syncturtle.services.file.service.collaborator.idempotency.IdempotencyRecordStore;
import com.syncturtle.services.file.service.collaborator.storage.ObjectStorageGateway;
import com.syncturtle.services.file.service.impl.FileAssetCleanupServiceImpl;
import com.syncturtle.services.file.service.result.IdempotencyAcquireResult;
import com.syncturtle.testing.annotation.JpaIntegrationTest;
import com.syncturtle.testing.annotation.UsePostgresDb;

import tools.jackson.databind.json.JsonMapper;

@JpaIntegrationTest
@UsePostgresDb("file_service_configuration_lifecycle_it")
@EnableJpaAuditing
@TestPropertySource(properties = {
        "spring.config.name=file-lifecycle-it",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FileLifecycleIT {

    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");
    private static final UUID OWNER_ID = UUID.fromString("c12c7988-b9fe-4299-a72f-e430d004d37b");
    private static final String ROUTE = "create-upload";
    private static final String REQUEST_HASH = "a".repeat(64);

    @Autowired
    private IdempotencyRecordRepository records;
    @Autowired
    private FileAssetRepository assets;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void deleteIsolatedTestData() {
        records.deleteAll();
        assets.deleteAll();
    }

    @Nested
    class Acquire {

        @Test
        void preservesCompletedReplayUntilTheExactTwentyFourHourBoundary() {
            String key = UUID.randomUUID().toString();
            IdempotencyAcquireResult created = storeAt(NOW).acquire(OWNER_ID, ROUTE, key, REQUEST_HASH);
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                IdempotencyRecord record = records.findByIdForUpdate(created.getRecordId()).orElseThrow();
                record.complete(created.getProcessingToken(), 200, "{\"asset\":\"existing\"}",
                        Duration.ofHours(24), clockAt(NOW));
            });

            IdempotencyAcquireResult beforeExpiry = storeAt(NOW.plus(Duration.ofHours(24)).minusNanos(1_000))
                    .acquire(OWNER_ID, ROUTE, key, REQUEST_HASH);
            IdempotencyAcquireResult atExpiry = storeAt(NOW.plus(Duration.ofHours(24)))
                    .acquire(OWNER_ID, ROUTE, key, REQUEST_HASH);

            assertThat(beforeExpiry.getDecision()).isEqualTo(IdempotencyAcquireResult.Decision.REPLAY);
            JsonMapper jsonMapper = JsonMapper.builder().build();
            assertThat(jsonMapper.readTree(beforeExpiry.getResponseBody()))
                    .isEqualTo(jsonMapper.readTree("{\"asset\":\"existing\"}"));
            assertThat(atExpiry.getDecision()).isEqualTo(IdempotencyAcquireResult.Decision.ACQUIRED);
            assertThat(atExpiry.getProcessingToken()).isNotEqualTo(created.getProcessingToken());
            IdempotencyRecord reclaimed = records.findById(created.getRecordId()).orElseThrow();
            assertThat(reclaimed.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(48)));
            assertThat(reclaimed.getResponseBody()).isNull();
        }

        @Test
        void honorsConfiguredProcessingLeaseWithoutChangingReplayHorizon() {
            String key = UUID.randomUUID().toString();
            IdempotencyAcquireResult created = storeAt(NOW).acquire(OWNER_ID, ROUTE, key, REQUEST_HASH);
            IdempotencyRecord original = records.findById(created.getRecordId()).orElseThrow();

            IdempotencyAcquireResult beforeLeaseExpiry = storeAt(NOW.plus(Duration.ofMinutes(2)).minusNanos(1_000))
                    .acquire(OWNER_ID, ROUTE, key, REQUEST_HASH);
            IdempotencyAcquireResult atLeaseExpiry = storeAt(NOW.plus(Duration.ofMinutes(2)))
                    .acquire(OWNER_ID, ROUTE, key, REQUEST_HASH);

            assertThat(original.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
            assertThat(original.getProcessingExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));
            assertThat(beforeLeaseExpiry.getDecision()).isEqualTo(IdempotencyAcquireResult.Decision.IN_PROGRESS);
            assertThat(atLeaseExpiry.getDecision()).isEqualTo(IdempotencyAcquireResult.Decision.ACQUIRED);
            assertThat(atLeaseExpiry.getProcessingToken()).isNotEqualTo(created.getProcessingToken());
        }
    }

    @Nested
    class MarkFailed {

        @Test
        void retainsFailedClaimForTwentyFourHoursFromFailure() {
            IdempotencyAcquireResult created = storeAt(NOW)
                    .acquire(OWNER_ID, ROUTE, UUID.randomUUID().toString(), REQUEST_HASH);
            Instant failedAt = NOW.plus(Duration.ofMinutes(1));

            storeAt(failedAt).markFailed(created.getRecordId(), created.getProcessingToken());

            IdempotencyRecord failed = records.findById(created.getRecordId()).orElseThrow();
            assertThat(failed.isFailed()).isTrue();
            assertThat(failed.getExpiresAt()).isEqualTo(failedAt.plus(Duration.ofHours(24)));
            assertThat(failed.getProcessingToken()).isNull();
        }
    }

    @Nested
    class CleanupDueAssets {

        @Test
        void deletesUnlinkedAssetAtSevenDaysAndKeepsYoungerAsset() {
            Instant sevenDaysAgo = NOW.minus(Duration.ofDays(7));
            FileAsset atBoundary = uploadedAsset(sevenDaysAgo);
            FileAsset younger = uploadedAsset(sevenDaysAgo.plusNanos(1_000));
            assets.saveAndFlush(atBoundary);
            assets.saveAndFlush(younger);
            ObjectStorageGateway storage = mock(ObjectStorageGateway.class);
            FileCleanupProperties properties = new FileCleanupProperties(true, 60_000, 10_000, 10,
                    Duration.ofMinutes(5));
            FileAssetCleanupServiceImpl service = new FileAssetCleanupServiceImpl(assets, records, storage,
                    properties, new TransactionTemplate(transactionManager), clockAt(NOW));

            FileAssetCleanupResponse result = service.cleanupDueAssets();

            assertThat(result.getAssetsClaimed()).isEqualTo(1);
            assertThat(result.getObjectsDeleted()).isEqualTo(1);
            assertThat(result.getObjectDeletionFailures()).isZero();
            assertThat(assets.findById(atBoundary.getId()).orElseThrow().getStorageDeletedAt()).isEqualTo(NOW);
            assertThat(assets.findById(younger.getId()).orElseThrow().isDeleted()).isFalse();
            verify(storage).deleteObjectVersions(atBoundary.getObjectKey());
            verify(storage, never()).deleteObjectVersions(younger.getObjectKey());
        }
    }

    private IdempotencyRecordStore storeAt(Instant instant) {
        FileUploadProperties properties = new FileUploadProperties(DataSize.ofMegabytes(5), Duration.ofMinutes(2));
        return new IdempotencyRecordStore(records, properties, transactionManager, clockAt(instant));
    }

    private FileAsset uploadedAsset(Instant uploadedAt) {
        UUID id = UUID.randomUUID();
        FileAsset asset = FileAsset.create(FileAssetCreateParam.builder()
                .id(id)
                .ownerUserId(OWNER_ID)
                .storageProvider("S3")
                .bucket("lifecycle-test")
                .objectKey("assets/" + id)
                .originalFilename("avatar.png")
                .contentType("image/png")
                .extension("png")
                .declaredSizeBytes(100)
                .purpose(FileAssetPurpose.USER_AVATAR)
                .uploadExpiresAt(uploadedAt.plus(Duration.ofMinutes(10)))
                .build());
        asset.markUploaded(100L, "version-1", "{}", clockAt(uploadedAt));
        return asset;
    }

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
