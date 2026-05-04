package com.syncturtle.common.contracts.api.error;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.syncturtle.common.core.error.ErrorCode;

public enum ApiErrorCode implements ErrorCode {

    BAD_REQUEST(1000, "BAD_REQUEST"),
    VALIDATION_FAILED(1001, "VALIDATION_FAILED"),
    UNAUTHORIZED(1002, "UNAUTHORIZED"),
    FORBIDDEN(1003, "FORBIDDEN"),
    NOT_FOUND(1004, "NOT_FOUND"),
    METHOD_NOT_ALLOWED(1005, "METHOD_NOT_ALLOWED"),
    UNSUPPORTED_MEDIA_TYPE(1006, "UNSUPPORTED_MEDIA_TYPE"),
    TOO_MANY_REQUESTS(1007, "TOO_MANY_REQUESTS"),
    INTERNAL_SERVER_ERROR(1099, "INTERNAL_SERVER_ERROR");

    private static final Map<Integer, ApiErrorCode> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(ApiErrorCode::getCode, Function.identity()));

    private final int code;
    private final String message;

    ApiErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static ApiErrorCode fromCodeOrDefault(int code) {
        return BY_CODE.getOrDefault(code, INTERNAL_SERVER_ERROR);
    }
}