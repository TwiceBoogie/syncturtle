package com.syncturtle.services.email.exceptions;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.spring.web.error.SyncturtleServiceException;

public final class EmailRuntimeConfigException extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;

    private EmailRuntimeConfigException(
            EmailErrorCode errorCode,
            HttpStatus status,
            String publicMessage,
            Map<String, Object> payload,
            Throwable cause) {
        super(errorCode, status, publicMessage, payload, cause);
        this.emailErrorCode = Objects.requireNonNull(errorCode, "errorCode is required");
    }

    public EmailErrorCode getEmailErrorCode() {
        return emailErrorCode;
    }

    public static EmailRuntimeConfigException incomplete() {
        return new EmailRuntimeConfigException(
                EmailErrorCode.EMAIL_RUNTIME_CONFIG_INCOMPLETE,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Email runtime config is incomplete.",
                Map.of(),
                null);
    }

    public static EmailRuntimeConfigException stale(long fetchedVersion, long requiredScopeVersion) {
        return new EmailRuntimeConfigException(
                EmailErrorCode.EMAIL_RUNTIME_CONFIG_STALE,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Fetched stale email runtime config.",
                Map.of(
                        "fetched_version", fetchedVersion,
                        "required_scope_version", requiredScopeVersion),
                null);
    }

    public static EmailRuntimeConfigException fetchFailed(Throwable cause) {
        return new EmailRuntimeConfigException(
                EmailErrorCode.EMAIL_RUNTIME_CONFIG_FETCH_FAILED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Failed to fetch email runtime config.",
                Map.of(),
                cause);
    }

}
