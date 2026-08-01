package com.syncturtle.services.file.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.file.exception.FileException;
import com.syncturtle.common.core.asset.AssetContentUrlFactory;
import com.syncturtle.services.file.configuration.property.StorageProperties;
import com.syncturtle.services.file.dto.request.FileUploadCreateRequest;
import com.syncturtle.services.file.dto.response.FileUploadCreateResponse;
import com.syncturtle.services.file.dto.response.PresignedPostResponse;
import com.syncturtle.services.file.model.FileAsset;
import com.syncturtle.services.file.model.IdempotencyRecord;
import com.syncturtle.services.file.model.param.FileAssetCreateParam;
import com.syncturtle.services.file.repository.FileAssetRepository;
import com.syncturtle.services.file.repository.IdempotencyRecordRepository;
import com.syncturtle.services.file.service.FileUploadService;
import com.syncturtle.services.file.service.collaborator.asset.FileAssetObjectKeyFactory;
import com.syncturtle.services.file.service.collaborator.asset.FileAssetPolicy;
import com.syncturtle.services.file.service.collaborator.asset.param.FileAssetObjectKeyCreateParam;
import com.syncturtle.services.file.service.collaborator.storage.ObjectStorageGateway;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private static final String STORAGE_PROVIDER = "S3";
    private static final String CREATE_UPLOAD_ROUTE_KEY = "POST /api/assets/v1/uploads";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final FileAssetPolicy assetPolicy;
    private final FileAssetRepository repository;
    private final IdempotencyRecordRepository idempotencyRepository;
    private final FileAssetObjectKeyFactory objectKeyFactory;
    private final AssetContentUrlFactory assetContentUrlFactory;
    private final ObjectStorageGateway storage;
    private final StorageProperties storageProperties;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Override
    @Transactional
    public FileUploadCreateResponse createUpload(UUID currentUserId, String idempotencyKey,
            FileUploadCreateRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(request, "file upload create request is required");

        assetPolicy.requireCreateAllowed(request.getPurpose(), request.getWorkspaceId(), request.getContentType(),
                request.getSizeBytes());

        if (!StringUtils.hasText(idempotencyKey)) {
            return createUploadWithoutIdempotency(currentUserId, request);
        }

        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String requestHash = requestHash(request);

        return idempotencyRepository
                .findByOwnerUserIdAndRouteKeyAndIdempotencyKey(currentUserId, CREATE_UPLOAD_ROUTE_KEY, normalizedKey)
                .map(existing -> replayOrReject(existing, requestHash))
                .orElseGet(() -> createUploadWithIdempotency(currentUserId, normalizedKey, requestHash, request));
    }

    @Override
    @Transactional
    public void completeUpload(UUID currentUserId, UUID assetId) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(assetId, "assetId is required");

        FileAsset asset = requireActiveAsset(assetId);

        assetPolicy.requireOwner(asset, currentUserId);

        if (asset.isUploaded()) {
            return;
        }

        try {
            Map<String, Object> metadata = storage.getObjectMetadata(asset.getObjectKey());

            assetPolicy.requireStorageObjectMatchesDeclaration(asset, metadata);

            Long actualSizeBytes = actualSizeBytes(metadata);
            String storageMetadata = toJson(metadata);

            asset.markUploaded(actualSizeBytes, storageMetadata, clock);
        } catch (FileException exception) {
            throw exception;
        } catch (Exception exception) {
            throw FileException.assetStorageMetadataFailed(assetId, exception);
        }
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

    private FileUploadCreateResponse createUploadWithIdempotency(UUID currentUserId, String idempotencyKey,
            String requestHash, FileUploadCreateRequest request) {
        IdempotencyRecord record = IdempotencyRecord.processing(idempotencyKey, currentUserId, CREATE_UPLOAD_ROUTE_KEY,
                requestHash, Instant.now(clock).plus(IDEMPOTENCY_TTL), clock);

        idempotencyRepository.save(record);

        FileUploadCreateResponse response = createUploadWithoutIdempotency(currentUserId, request);

        record.complete(200, toJson(response), clock);

        return response;
    }

    private FileUploadCreateResponse createUploadWithoutIdempotency(UUID currentUserId,
            FileUploadCreateRequest request) {
        UUID assetId = UUID.randomUUID();
        Duration expiration = storageProperties.getPresignExpiration();
        Instant uploadExpiresAt = Instant.now(clock).plus(expiration);

        String objectKey = objectKeyFactory.createObjectKey(FileAssetObjectKeyCreateParam.builder()
                .assetId(assetId)
                .workspaceId(request.getWorkspaceId())
                .ownerUserId(currentUserId)
                .purpose(request.getPurpose())
                .originalFilename(request.getOriginalFilename())
                .build());

        FileAsset asset = FileAsset.create(FileAssetCreateParam.builder()
                .id(assetId)
                .workspaceId(request.getWorkspaceId())
                .ownerUserId(currentUserId)
                .storageProvider(STORAGE_PROVIDER)
                .bucket(storageProperties.getBucket())
                .objectKey(objectKey)
                .originalFilename(request.getOriginalFilename())
                .contentType(request.getContentType())
                .extension(extensionOf(request.getOriginalFilename()))
                .declaredSizeBytes(request.getSizeBytes())
                .purpose(request.getPurpose())
                .uploadExpiresAt(uploadExpiresAt)
                .build());

        FileAsset saved = repository.save(asset);

        PresignedPostResponse uploadData = storage.generatePresignedPost(
                saved.getObjectKey(),
                saved.getContentType(),
                saved.getDeclaredSizeBytes(),
                expiration);

        return FileUploadCreateResponse.builder()
                .assetId(saved.getId())
                .assetUrl(assetContentUrlFactory.staticAssetUrl(saved.getId()))
                .uploadExpiresAt(uploadExpiresAt)
                .uploadData(uploadData)
                .build();
    }

    private FileUploadCreateResponse replayOrReject(IdempotencyRecord existing, String requestHash) {
        if (!existing.requestHashMatches(requestHash)) {
            // throw FileException.idempotencyKeyConflict();
        }

        if (!existing.isCompleted()) {
            // throw FileException.idempotencyRequestInProgress();
        }

        try {
            return jsonMapper.readValue(existing.getResponseBody(), FileUploadCreateResponse.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not deeserialize idempotent file upload response.", exception);
        }
    }

    private FileAsset requireActiveAsset(UUID assetId) {
        Assert.notNull(assetId, "assetId is required");

        return repository.findByIdAndDeletedFlagFalse(assetId)
                .orElseThrow(() -> FileException.assetNotFound(assetId));
    }

    private Long actualSizeBytes(Map<String, Object> metadata) {
        Object value = metadata.get("ContentLength");

        if (value instanceof Number number) {
            return number.longValue();
        }

        return null;
    }

    private String requestHash(FileUploadCreateRequest request) {
        try {
            String json = jsonMapper.writeValueAsString(request);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash file upload request.", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize value as JSON.", exception);
        }
    }

    private static String normalizeIdempotencyKey(String value) {
        Assert.hasText(value, "idempotencyKey is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= 128, "idempotencyKey must be 128 characters or fewer");
        Assert.isTrue(!normalized.contains(":"), "idempotencyKey must not contain ':");
        Assert.isTrue(!normalized.contains(" "), "idempotencyKey must not contain spaces");

        return normalized;
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return null;
        }

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }

        return filename.substring(dotIndex + 1).toLowerCase();
    }

}
