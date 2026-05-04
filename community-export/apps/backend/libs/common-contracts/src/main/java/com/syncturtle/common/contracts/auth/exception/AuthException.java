package com.syncturtle.common.contracts.auth.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.core.exceptions.SyncturtleException;

public final class AuthException extends SyncturtleException {

    private final AuthErrorCode authErrorCode;

    public AuthException(AuthErrorCode errorCode) {
        this(errorCode, Map.of(), null);
    }

    public AuthException(AuthErrorCode errorCode, Map<String, Object> payload) {
        this(errorCode, payload, null);
    }

    public AuthException(AuthErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(errorCode, payload, cause);
        this.authErrorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    public AuthErrorCode getAuthErrorCode() {
        return authErrorCode;
    }

    public static AuthException of(AuthErrorCode errorCode) {
        return new AuthException(errorCode);
    }

    public AuthException with(String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(getPayload());

        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }

        return new AuthException(this.authErrorCode, copy);
    }

}
