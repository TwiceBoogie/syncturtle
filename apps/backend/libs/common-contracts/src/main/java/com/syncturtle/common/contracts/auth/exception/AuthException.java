package com.syncturtle.common.contracts.auth.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.core.exceptions.SyncturtleServiceException;

public final class AuthException extends SyncturtleServiceException {

    private final AuthErrorCode authErrorCode;

    private AuthException(AuthErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(errorCode, payload, cause);
        this.authErrorCode = Objects.requireNonNull(errorCode, "errorCode is required");
    }

    public AuthErrorCode getAuthErrorCode() {
        return authErrorCode;
    }

    public static AuthException of(AuthErrorCode errorCode) {
        return new AuthException(errorCode, Map.of(), null);
    }

    public static AuthException of(AuthErrorCode errorCode, Map<String, Object> payload) {
        return new AuthException(errorCode, payload, null);
    }

    public static AuthException of(AuthErrorCode errorCode, Throwable cause) {
        return new AuthException(errorCode, Map.of(), cause);
    }

    public AuthException with(String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(getPayload());

        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }

        return new AuthException(authErrorCode, copy, getCause());
    }

}
