package com.syncturtle.common.core.exceptions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.core.enums.AuthErrorCode;

public final class AuthenticationException extends RuntimeException {

    private final AuthErrorCode errorCode;
    private final Map<String, Object> payload;

    public AuthenticationException(AuthErrorCode errorCode) {
        this(errorCode, Map.of());
    }

    public AuthenticationException(AuthErrorCode errorCode, Map<String, Object> payload) {
        super(Objects.requireNonNull(errorCode, "errorCode").getMessage());

        this.errorCode = errorCode;
        Map<String, Object> copy = new LinkedHashMap<>();
        if (payload != null) {
            copy.putAll(payload);
        }
        this.payload = Collections.unmodifiableMap(copy);
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Map<String, Object> getErrorMap() {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error_code", errorCode.getCode());
        error.put("error_message", errorCode.getMessage());
        error.putAll(payload);
        return Collections.unmodifiableMap(error);
    }

    public static AuthenticationException of(AuthErrorCode errorCode) {
        return new AuthenticationException(errorCode);
    }

    public AuthenticationException with(String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(this.payload);
        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }
        return new AuthenticationException(this.errorCode, copy);
    }

}
