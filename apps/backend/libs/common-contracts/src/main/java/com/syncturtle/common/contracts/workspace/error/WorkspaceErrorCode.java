package com.syncturtle.common.contracts.workspace.error;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.syncturtle.common.core.error.ErrorCode;

public enum WorkspaceErrorCode implements ErrorCode {

    WORKSPACE_NOT_FOUND(
            4000,
            "WORKSPACE_NOT_FOUND",
            404,
            "Workspace was not found."),

    WORKSPACE_SLUG_REQUIRED(
            4001,
            "WORKSPACE_SLUG_REQUIRED",
            400,
            "Workspace slug is required."),

    WORKSPACE_SLUG_INVALID(
            4002,
            "WORKSPACE_SLUG_INVALID",
            400,
            "Workspace slug is invalid."),

    WORKSPACE_SLUG_ALREADY_EXISTS(
            4003,
            "WORKSPACE_SLUG_ALREADY_EXISTS",
            409,
            "Workspace slug already exists."),

    WORKSPACE_OWNER_NOT_FOUND(
            4004,
            "WORKSPACE_OWNER_NOT_FOUND",
            404,
            "Workspace owner was not found."),

    WORKSPACE_LOGO_ASSET_INVALID(
            4005,
            "WORKSPACE_LOGO_ASSET_INVALID",
            400,
            "Workspace logo asset is invalid."),

    WORKSPACE_LOGO_ASSET_NOT_UPLOADED(
            4006,
            "WORKSPACE_LOGO_ASSET_NOT_UPLOADED",
            409,
            "Workspace logo asset has not been uploaded."),

    WORKSPACE_LOGO_ASSET_FORBIDDEN(
            4007,
            "WORKSPACE_LOGO_ASSET_FORBIDDEN",
            403,
            "Workspace logo cannot be used for this workspace."),

    WORKSPACE_CREATE_FAILED(
            4099,
            "WORKSPACE_CREATE_FAILED",
            500,
            "Could not create workspace.");

    private static final Map<Integer, WorkspaceErrorCode> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(WorkspaceErrorCode::getCode, Function.identity()));

    private final int code;
    private final String key;
    private final int httpStatusCode;
    private final String publicMessage;

    WorkspaceErrorCode(int code, String key, int httpStatusCode, String publicMessage) {
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

    public static WorkspaceErrorCode fromCodeOrDefault(int code) {
        return BY_CODE.getOrDefault(code, WORKSPACE_CREATE_FAILED);
    }

}