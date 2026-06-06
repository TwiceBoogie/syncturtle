package com.syncturtle.common.contracts.api.error;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.syncturtle.common.core.error.ErrorCode;

public enum ApiErrorCode implements ErrorCode {

    VALIDATION_FAILED(
            1000,
            "VALIDATION_FAILED",
            400,
            "One or more fields are invalid."),

    BAD_REQUEST(
            1001,
            "BAD_REQUEST",
            400,
            "Request is invalid."),

    UNAUTHORIZED(
            1002,
            "UNAUTHORIZED",
            401,
            "Authentication is required."),

    FORBIDDEN(
            1003,
            "FORBIDDEN",
            403,
            "You do not have permission to perform this action."),

    NOT_FOUND(
            1004,
            "NOT_FOUND",
            404,
            "Resource was not found."),

    CONFLICT(
            1005,
            "CONFLICT",
            409,
            "Request conflicts with current resource state."),

    METHOD_NOT_ALLOWED(
            1006,
            "METHOD_NOT_ALLOWED",
            405,
            "HTTP method is not allowed for this endpoint."),

    UNSUPPORTED_MEDIA_TYPE(
            1007,
            "UNSUPPORTED_MEDIA_TYPE",
            415,
            "Content type is not supported."),

    TOO_MANY_REQUESTS(
            1008,
            "TOO_MANY_REQUESTS",
            429,
            "Too many requests."),

    REQUEST_TIMEOUT(
            1009,
            "REQUEST_TIMEOUT",
            408,
            "Request timed out."),

    REMOTE_SERVICE_FAILED(
            1100,
            "REMOTE_SERVICE_FAILED",
            502,
            "A downstream service failed."),

    REMOTE_SERVICE_UNAVAILABLE(
            1101,
            "REMOTE_SERVICE_UNAVAILABLE",
            503,
            "A downstream service is unavailable."),

    REMOTE_SERVICE_TIMEOUT(
            1102,
            "REMOTE_SERVICE_TIMEOUT",
            504,
            "A downstream service timed out."),

    DATA_INTEGRITY_VIOLATION(
            1200,
            "DATA_INTEGRITY_VIOLATION",
            409,
            "Request conflicts with existing data."),

    OPTIMISTIC_LOCK_CONFLICT(
            1201,
            "OPTIMISTIC_LOCK_CONFLICT",
            409,
            "Resource was modified by another request."),

    INTERNAL_SERVER_ERROR(
            1099,
            "INTERNAL_SERVER_ERROR",
            500,
            "Something went wrong.");

    private static final Map<Integer, ApiErrorCode> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ApiErrorCode::getCode, Function.identity()));

    private final int code;
    private final String key;
    private final int httpStatusCode;
    private final String publicMessage;

    ApiErrorCode(int code, String key, int httpStatusCode, String publicMessage) {
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

    public static ApiErrorCode fromCodeOrDefault(int code) {
        return BY_CODE.getOrDefault(code, INTERNAL_SERVER_ERROR);
    }

}