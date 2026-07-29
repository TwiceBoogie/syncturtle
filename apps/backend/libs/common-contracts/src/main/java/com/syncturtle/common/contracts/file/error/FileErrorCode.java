package com.syncturtle.common.contracts.file.error;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.syncturtle.common.core.error.ErrorCode;

public enum FileErrorCode implements ErrorCode {

    FILE_ASSET_NOT_FOUND(
            8000,
            "FILE_ASSET_NOT_FOUND",
            404,
            "File asset was not found."),
    FILE_ASSET_NOT_UPLOADED(
            8001,
            "FILE_ASSET_NOT_UPLOADED",
            409,
            "File asset has not been uploaded."),
    FILE_ASSET_NOT_STATIC_DISPLAY_ASSET(
            8002,
            "FILE_ASSET_NOT_STATIC_DISPLAY_ASSET",
            400,
            "File asset cannot be used as a static display asset."),
    FILE_ASSET_ACCESS_DENIED(
            8003,
            "FILE_ASSET_ACCESS_DENIED",
            403,
            "You do not have access to this file asset."),
    FILE_ASSET_PURPOSE_INVALID(
            8004,
            "FILE_ASSET_PURPOSE_INVALID",
            400,
            "File asset purpose is invalid."),
    FILE_ASSET_WORKSPACE_MISMATCH(
            8005,
            "FILE_ASSET_WORKSPACE_MISMATCH",
            400,
            "File asset does not belong to this workspace."),
    FILE_ASSET_OWNER_MISMATCH(
            8006,
            "FILE_ASSET_OWNER_MISMATCH",
            403,
            "File asset does not belong to this user."),
    FILE_ASSET_UPLOAD_EXPIRED(
            8007,
            "FILE_ASSET_UPLOAD_EXPIRED",
            410,
            "File asset upload has expired."),
    FILE_ASSET_UPLOAD_SCOPE_INVALID(
            8008,
            "FILE_ASSET_UPLOAD_SCOPE_INVALID",
            400,
            "File asset upload scope is invalid."),
    FILE_ASSET_CONTENT_TYPE_UNSUPPORTED(
            8009,
            "FILE_ASSET_CONTENT_TYPE_UNSUPPORTED",
            415,
            "File type is not supported."),
    FILE_ASSET_TOO_LARGE(
            8010,
            "FILE_ASSET_TOO_LARGE",
            413,
            "File is too large."),
    FILE_ASSET_STORAGE_METADATA_FAILED(
            8011,
            "FILE_ASSET_STORAGE_METADATA_FAILED",
            502,
            "Could not read file asset storage metadata."),
    FILE_ASSET_STORAGE_METADATA_INVALID(
            8012,
            "FILE_ASSET_STORAGE_METADATA_INVALID",
            409,
            "File asset storage metadata is invalid."),
    FILE_ASSET_SIGNED_URL_FAILED(
            8013,
            "FILE_ASSET_SIGNED_URL_FAILED",
            502,
            "Could not create file asset URL."),
    FILE_ASSET_LINK_NOT_FOUND(
            8020,
            "FILE_ASSET_LINK_NOT_FOUND",
            404,
            "File asset link was not found."),
    FILE_ASSET_LINK_FAILED(
            8021,
            "FILE_ASSET_LINK_FAILED",
            500,
            "Could not link file asset."),
    IDEMPOTENCY_KEY_CONFLICT(
            8030,
            "IDEMPOTENCY_KEY_CONFLICT",
            409,
            "Idempotency key was already used for a different request."),
    IDEMPOTENCY_REQUEST_IN_PROGRESS(
            8031,
            "IDEMPOTENCY_REQUEST_IN_PROGRESS",
            409,
            "Request is still being processed."),
    IDEMPOTENCY_REPLAY_FAILED(
            8032,
            "IDEMPOTENCY_REPLAY_FAILED",
            500,
            "Could not replay idempotent response."),
    FILE_OPERATION_FAILED(
            8099,
            "FILE_OPERATION_FAILED",
            500,
            "File operation failed.");

    private static final Map<Integer, FileErrorCode> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(FileErrorCode::getCode, Function.identity()));

    private final int code;
    private final String key;
    private final int httpStatusCode;
    private final String publicMessage;

    FileErrorCode(int code, String key, int httpStatusCode, String publicMessage) {
        this.code = code;
        this.key = key;
        this.httpStatusCode = httpStatusCode;
        this.publicMessage = publicMessage;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String getPublicMessage() {
        return publicMessage;
    }

    public static FileErrorCode fromCode(int code) {
        FileErrorCode value = BY_CODE.get(code);

        if (value == null) {
            throw new IllegalArgumentException("Unknown FileErrorCode: " + code);
        }

        return value;
    }

    public static FileErrorCode fromCodeOrDefault(int code) {
        return BY_CODE.getOrDefault(code, FILE_OPERATION_FAILED);
    }

}
