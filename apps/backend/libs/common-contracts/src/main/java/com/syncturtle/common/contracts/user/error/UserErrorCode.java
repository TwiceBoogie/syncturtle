package com.syncturtle.common.contracts.user.error;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.syncturtle.common.core.error.ErrorCode;

public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(
            3000,
            "USER_NOT_FOUND",
            404,
            "User was not found."),

    USER_PROFILE_UPDATE_FAILED(
            3001,
            "USER_PROFILE_UPDATE_FAILED",
            500,
            "Could not update user profile."),

    USER_PROFILE_NOT_FOUND(
            3002,
            "USER_PROFILE_NOT_FOUND",
            404,
            "User profile was not found."),

    USER_ASSET_INVALID(
            3010,
            "USER_ASSET_INVALID",
            400,
            "User asset is invalid."),

    USER_ASSET_NOT_UPLOADED(
            3011,
            "USER_ASSET_NOT_UPLOADED",
            409,
            "User asset has not been uploaded."),

    USER_ASSET_FORBIDDEN(
            3012,
            "USER_ASSET_FORBIDDEN",
            403,
            "User asset cannot be used for this user."),

    USER_OPERATION_FAILED(
            3099,
            "USER_OPERATION_FAILED",
            500,
            "User operation failed.");

    private static final Map<Integer, UserErrorCode> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(UserErrorCode::getCode, Function.identity()));

    private final int code;
    private final String key;
    private final int httpStatusCode;
    private final String publicMessage;

    UserErrorCode(int code, String key, int httpStatusCode, String publicMessage) {
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

    public static UserErrorCode fromCode(int code) {
        UserErrorCode value = BY_CODE.get(code);

        if (value == null) {
            throw new IllegalArgumentException("Unknown UserErrorCode: " + code);
        }

        return value;
    }

    public static UserErrorCode fromCodeOrDefault(int code) {
        return BY_CODE.getOrDefault(code, USER_OPERATION_FAILED);
    }

}