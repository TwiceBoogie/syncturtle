package com.syncturtle.services.file.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.file.exception.FileException;
import com.syncturtle.common.core.asset.AssetContentUrlFactory;
import com.syncturtle.services.file.configuration.property.FileUploadProperties;
import com.syncturtle.services.file.configuration.property.StorageProperties;
import com.syncturtle.services.file.dto.response.FileUploadCreateResponse;
import com.syncturtle.services.file.dto.response.PresignedPostResponse;
import com.syncturtle.services.file.model.FileAsset;
import com.syncturtle.services.file.model.IdempotencyRecord;
import com.syncturtle.services.file.model.param.FileAssetCreateParam;
import com.syncturtle.services.file.repository.FileAssetRepository;
import com.syncturtle.services.file.service.FileUploadService;
import com.syncturtle.services.file.service.collaborator.asset.FileAssetObjectKeyFactory;
import com.syncturtle.services.file.service.collaborator.asset.FileAssetPolicy;
import com.syncturtle.services.file.service.collaborator.asset.param.FileAssetObjectKeyCreateParam;
import com.syncturtle.services.file.service.collaborator.idempotency.IdempotencyRecordStore;
import com.syncturtle.services.file.service.collaborator.storage.ObjectStorageGateway;
import com.syncturtle.services.file.service.param.FileUploadCreateParam;
import com.syncturtle.services.file.service.result.FileUploadCreateResult;
import com.syncturtle.services.file.service.result.IdempotencyAcquireResult;
import com.syncturtle.services.file.service.result.PresignedPostResult;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private static final String STORAGE_PROVIDER = "S3";
    private static final String CREATE_UPLOAD_ROUTE_KEY = "POST /api/assets/v1/uploads";

    private final FileAssetPolicy assetPolicy;
    private final FileAssetRepository assetRepository;
    private final IdempotencyRecordStore idempotencyStore;
    private final FileAssetObjectKeyFactory objectKeyFactory;
    private final AssetContentUrlFactory assetContentUrlFactory;
    private final ObjectStorageGateway storage;
    private final StorageProperties storageProperties;
    private final FileUploadProperties uploadProperties;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    public FileUploadCreateResponse createUpload(FileUploadCreateParam param) {
        Assert.notNull(param, "file upload create param is required");

        assetPolicy.requireCreateAllowed(
                param.getPurpose(),
                param.getWorkspaceId(),
                param.getContentType(),
                param.getSizeBytes());

        if (!param.hasIdempotencyKey()) {
            FileUploadCreateResult result = requireResult(
                    transactionTemplate.execute(status -> createUploadInTransaction(param)));
            return toResponse(result);
        }

        String requestHash = requestHash(param);
        IdempotencyAcquireResult acquisition = idempotencyStore.acquire(
                param.getCurrentUserId(),
                CREATE_UPLOAD_ROUTE_KEY,
                param.getIdempotencyKey(),
                requestHash);

        if (acquisition.getDecision() == IdempotencyAcquireResult.Decision.CONFLICT) {
            throw FileException.idempotencyKeyConflict();
        }

        if (acquisition.getDecision() == IdempotencyAcquireResult.Decision.IN_PROGRESS) {
            throw FileException.idempotencyRequestInProgress();
        }

        if (acquisition.getDecision() == IdempotencyAcquireResult.Decision.REPLAY) {
            return replay(acquisition.getResponseBody());
        }

        return createClaimedUpload(param, acquisition);
    }

    @Override
    public void completeUpload(UUID currentUserId, UUID assetId) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(assetId, "assetId is required");

        FileAsset snapshot = requireActiveAsset(assetId);
        assetPolicy.requireOwner(snapshot, currentUserId);

        if (snapshot.isUploaded()) {
            return;
        }

        if (snapshot.isUploadExpired(clock)) {
            throw FileException.assetUploadExpired(snapshot.getId());
        }

        Map<String, Object> metadata = readStorageMetadata(snapshot);
        String storageVersionId = storageVersionId(metadata);
        byte[] objectPrefix = readStoragePrefix(snapshot, storageVersionId);

        transactionTemplate.executeWithoutResult(
                status -> completeUploadInTransaction(
                        currentUserId,
                        assetId,
                        metadata,
                        storageVersionId,
                        objectPrefix));
    }

    @Override
    @Transactional
    public void deleteAsset(UUID currentUserId, UUID assetId) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(assetId, "assetId is required");

        FileAsset asset = requireActiveAsset(assetId);
        assetPolicy.requireOwner(asset, currentUserId);
        asset.markDeleted(clock);
    }

    private FileUploadCreateResult createUploadInTransaction(FileUploadCreateParam param) {
        UUID assetId = UUID.randomUUID();
        Duration expiration = storageProperties.getPresignExpiration();
        Instant uploadExpiresAt = Instant.now(clock).plus(expiration);

        String objectKey = objectKeyFactory.createObjectKey(FileAssetObjectKeyCreateParam.builder()
                .assetId(assetId)
                .workspaceId(param.getWorkspaceId())
                .ownerUserId(param.getCurrentUserId())
                .purpose(param.getPurpose())
                .originalFilename(param.getOriginalFilename())
                .build());

        FileAsset asset = FileAsset.create(FileAssetCreateParam.builder()
                .id(assetId)
                .workspaceId(param.getWorkspaceId())
                .ownerUserId(param.getCurrentUserId())
                .storageProvider(STORAGE_PROVIDER)
                .bucket(storageProperties.getBucket())
                .objectKey(objectKey)
                .originalFilename(param.getOriginalFilename())
                .contentType(param.getContentType())
                .extension(extensionOf(param.getOriginalFilename()))
                .declaredSizeBytes(param.getSizeBytes())
                .purpose(param.getPurpose())
                .uploadExpiresAt(uploadExpiresAt)
                .build());

        FileAsset saved = assetRepository.save(asset);
        PresignedPostResult uploadData = storage.generatePresignedPost(
                saved.getObjectKey(),
                saved.getContentType(),
                saved.getDeclaredSizeBytes(),
                expiration);

        return FileUploadCreateResult.builder()
                .assetId(saved.getId())
                .assetUrl(assetContentUrlFactory.staticAssetUrl(saved.getId()))
                .uploadExpiresAt(uploadExpiresAt)
                .uploadData(uploadData)
                .build();
    }

    private String requestHash(FileUploadCreateParam param) {
        String canonicalRequest = param.getPurpose().name()
                + "\n" + Objects.toString(param.getWorkspaceId(), "")
                + "\n" + param.getOriginalFilename()
                + "\n" + param.getContentType()
                + "\n" + param.getSizeBytes();

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash file upload request.", exception);
        }
    }

    private FileUploadCreateResponse replay(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw FileException
                    .idempotencyReplayFailed(new IllegalStateException("Stored idempotency response is empty"));
        }

        try {
            return jsonMapper.readValue(responseBody, FileUploadCreateResponse.class);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw FileException.idempotencyReplayFailed(exception);
        }
    }

    private FileUploadCreateResponse createClaimedUpload(FileUploadCreateParam param,
            IdempotencyAcquireResult acquisition) {
        UUID recordId = Objects.requireNonNull(acquisition.getRecordId(), "idempotency recordId is required");
        UUID processingToken = Objects.requireNonNull(acquisition.getProcessingToken(),
                "idempotency processingToken is required");

        try {
            FileUploadCreateResponse response = transactionTemplate.execute(status -> {
                IdempotencyRecord record = idempotencyStore.requireClaimedForUpdate(recordId, processingToken);
                FileUploadCreateResult created = createUploadInTransaction(param);
                FileUploadCreateResponse boundaryResponse = toResponse(created);
                String responseBody = toJson(boundaryResponse);

                record.complete(processingToken, 200, responseBody, uploadProperties.getIdempotencyRetention(), clock);

                return boundaryResponse;
            });

            return Objects.requireNonNull(response, "file upload create response is required");
        } catch (RuntimeException exception) {
            idempotencyStore.markFailed(recordId, processingToken);
            throw exception;
        }
    }

    private Map<String, Object> readStorageMetadata(FileAsset asset) {
        try {
            return storage.getObjectMetadata(asset.getObjectKey());
        } catch (FileException exception) {
            throw exception;
        } catch (Exception exception) {
            throw FileException.assetStorageMetadataFailed(asset.getId(), exception);
        }
    }

    private byte[] readStoragePrefix(FileAsset asset, String storageVersionId) {
        try {
            return storage.getObjectPrefix(asset.getObjectKey(), storageVersionId, 32);
        } catch (FileException exception) {
            throw exception;
        } catch (Exception exception) {
            throw FileException.assetStorageMetadataFailed(asset.getId(), exception);
        }
    }

    private void completeUploadInTransaction(
            UUID currentUserId,
            UUID assetId,
            Map<String, Object> metadata,
            String storageVersionId,
            byte[] objectPrefix) {
        FileAsset asset = assetRepository.findByIdForUpdate(assetId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> FileException.assetNotFound(assetId));

        assetPolicy.requireOwner(asset, currentUserId);

        if (asset.isUploaded()) {
            return;
        }

        if (asset.isUploadExpired(clock)) {
            throw FileException.assetUploadExpired(asset.getId());
        }

        assetPolicy.requireStorageObjectMatchesDeclaration(asset, metadata, objectPrefix);
        asset.markUploaded(actualSizeBytes(metadata), storageVersionId, toJson(metadata), clock);
    }

    private FileAsset requireActiveAsset(UUID assetId) {
        Assert.notNull(assetId, "assetId is required");

        return assetRepository.findByIdAndDeletedFlagFalse(assetId)
                .orElseThrow(() -> FileException.assetNotFound(assetId));
    }

    private Long actualSizeBytes(Map<String, Object> metadata) {
        Object value = metadata.get("ContentLength");

        if (value instanceof Number number) {
            return number.longValue();
        }

        return null;
    }

    private String storageVersionId(Map<String, Object> metadata) {
        Object value = metadata.get("VersionId");
        if (value instanceof String versionId && StringUtils.hasText(versionId)) {
            String normalizedVersionId = versionId.trim();
            if (!"null".equalsIgnoreCase(normalizedVersionId)) {
                return normalizedVersionId;
            }
        }
        throw new IllegalStateException("Versioned object storage is required for completed uploads.");
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize value as JSON.", exception);
        }
    }

    private static FileUploadCreateResult requireResult(FileUploadCreateResult result) {
        return Objects.requireNonNull(result, "file upload create result is required");
    }

    private static String extensionOf(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }

    private static FileUploadCreateResponse toResponse(FileUploadCreateResult result) {
        PresignedPostResult uploadData = result.getUploadData();
        PresignedPostResponse uploadResponse = new PresignedPostResponse(uploadData.getUrl(), uploadData.getFields());

        return FileUploadCreateResponse.builder()
                .assetId(result.getAssetId())
                .assetUrl(result.getAssetUrl())
                .uploadExpiresAt(result.getUploadExpiresAt())
                .uploadData(uploadResponse)
                .build();
    }

}
