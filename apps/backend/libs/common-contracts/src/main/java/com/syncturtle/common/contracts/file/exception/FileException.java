package com.syncturtle.common.contracts.file.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.syncturtle.common.contracts.file.error.FileErrorCode;
import com.syncturtle.common.core.exception.SyncturtleServiceException;

public final class FileException extends SyncturtleServiceException {

    private final FileErrorCode fileErrorCode;

    private FileException(FileErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(errorCode, payload, cause);
        this.fileErrorCode = Objects.requireNonNull(errorCode, "errorCode is required");
    }

    public FileErrorCode getFileErrorCode() {
        return fileErrorCode;
    }

    public static FileException of(FileErrorCode errorCode) {
        return new FileException(errorCode, Map.of(), null);
    }

    public static FileException of(FileErrorCode errorCode, Map<String, Object> payload) {
        return new FileException(errorCode, payload, null);
    }

    public static FileException of(FileErrorCode errorCode, Throwable cause) {
        return new FileException(errorCode, Map.of(), cause);
    }

    public static FileException assetNotFound(UUID assetId) {
        return new FileException(FileErrorCode.FILE_ASSET_NOT_FOUND, Map.of("asset_id", assetId), null);
    }

    public static FileException assetNotUploaded(UUID assetId) {
        return new FileException(FileErrorCode.FILE_ASSET_NOT_UPLOADED, Map.of("asset_id", assetId), null);
    }

    public static FileException assetNotStaticDisplayAsset(UUID assetId) {
        return new FileException(FileErrorCode.FILE_ASSET_NOT_STATIC_DISPLAY_ASSET, Map.of("asset_id", assetId), null);
    }

    public static FileException assetAccessDenied(UUID assetId) {
        return new FileException(FileErrorCode.FILE_ASSET_ACCESS_DENIED, Map.of("asset_id", assetId), null);
    }

    public static FileException assetPurposeInvalid(UUID assetId) {
        return new FileException(FileErrorCode.FILE_ASSET_PURPOSE_INVALID, Map.of("asset_id", assetId), null);
    }

    public static FileException assetWorkspaceMismatch(UUID assetId, UUID workspaceId) {
        return new FileException(FileErrorCode.FILE_ASSET_WORKSPACE_MISMATCH,
                Map.of("asset_id", assetId, "workspace_id", workspaceId), null);
    }

    public static FileException assetOwnerMismatch(UUID assetId, UUID ownerUserId) {
        return new FileException(FileErrorCode.FILE_ASSET_OWNER_MISMATCH,
                Map.of("asset_id", assetId, "owner_user_id", ownerUserId), null);
    }

    public static FileException assetUploadExpired(UUID assetId) {
        return new FileException(FileErrorCode.FILE_ASSET_UPLOAD_EXPIRED, Map.of("asset_id", assetId), null);
    }

    public static FileException uploadScopeInvalid(String reason) {
        return new FileException(FileErrorCode.FILE_ASSET_UPLOAD_SCOPE_INVALID, Map.of("reason", reason), null);
    }

    public static FileException unsupportedContentType(String contentType) {
        return new FileException(FileErrorCode.FILE_ASSET_CONTENT_TYPE_UNSUPPORTED, Map.of("content_type", contentType),
                null);
    }

    public static FileException assetTooLarge(long sizeBytes, long maxSizeBytes) {
        return new FileException(FileErrorCode.FILE_ASSET_TOO_LARGE,
                Map.of("size_bytes", sizeBytes, "max_size_bytes", maxSizeBytes), null);
    }

    public static FileException assetStorageMetadataFailed(UUID assetId, Throwable cause) {
        return new FileException(FileErrorCode.FILE_ASSET_STORAGE_METADATA_FAILED, Map.of("asset_id", assetId), cause);
    }

    public static FileException assetStorageMetadataInvalid(UUID assetId) {
        return new FileException(FileErrorCode.FILE_ASSET_STORAGE_METADATA_INVALID, Map.of("asset_id", assetId), null);
    }

    public static FileException assetSignedUrlFailed(UUID assetId, Throwable cause) {
        return new FileException(FileErrorCode.FILE_ASSET_SIGNED_URL_FAILED, Map.of("asset_id", assetId), cause);
    }

    public static FileException assetLinkFailed(UUID assetId, Throwable cause) {
        return new FileException(FileErrorCode.FILE_ASSET_LINK_FAILED, Map.of("asset_id", assetId), cause);
    }

    public static FileException idempotencyKeyConflict() {
        return new FileException(FileErrorCode.IDEMPOTENCY_KEY_CONFLICT, Map.of(), null);
    }

    public static FileException idempotencyRequestInProgress() {
        return new FileException(FileErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS, Map.of(), null);
    }

    public static FileException idempotencyReplayFailed(Throwable cause) {
        return new FileException(FileErrorCode.IDEMPOTENCY_REPLAY_FAILED, Map.of(), cause);
    }

    public FileException with(String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(getPayload());

        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }

        return new FileException(fileErrorCode, copy, getCause());
    }

}
