package com.syncturtle.common.core.exceptions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.core.error.ErrorCode;

public class SyncturtleException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> payload;

    public SyncturtleException(ErrorCode errorCode) {
        this(errorCode, Map.of(), null);
    }

    public SyncturtleException(ErrorCode errorCode, Map<String, Object> payload) {
        this(errorCode, payload, null);
    }

    public SyncturtleException(ErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(Objects.requireNonNull(errorCode, "errorCode").getKey(), cause);

        this.errorCode = errorCode;

        Map<String, Object> copy = new LinkedHashMap<>();
        if (payload != null) {
            copy.putAll(payload);
        }

        this.payload = Collections.unmodifiableMap(copy);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getCode() {
        return errorCode.getCode();
    }

    public String getErrorMessage() {
        return errorCode.getKey();
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Map<String, Object> getErrorMap() {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error_code", errorCode.getCode());
        error.put("error_message", errorCode.getKey());
        error.putAll(payload);

        return Collections.unmodifiableMap(error);
    }

}
