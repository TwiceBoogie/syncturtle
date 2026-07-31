package com.syncturtle.services.email.exception;

import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.core.exception.SyncturtleServiceException;

public final class EmailRuntimeConfigException extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;

    private EmailRuntimeConfigException(EmailErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(errorCode, payload, cause);
        this.emailErrorCode = Objects.requireNonNull(errorCode, "errorCode is required");
    }

    public EmailErrorCode getEmailErrorCode() {
        return emailErrorCode;
    }

    public static EmailRuntimeConfigException incomplete() {
        return new EmailRuntimeConfigException(EmailErrorCode.EMAIL_RUNTIME_CONFIG_INCOMPLETE, Map.of(), null);
    }

    public static EmailRuntimeConfigException stale(long fetchedVersion, long requiredScopeVersion) {
        return new EmailRuntimeConfigException(
                EmailErrorCode.EMAIL_RUNTIME_CONFIG_STALE,
                Map.of(
                        "fetched_version", fetchedVersion,
                        "required_scope_version", requiredScopeVersion),
                null);
    }

    public static EmailRuntimeConfigException fetchFailed(Throwable cause) {
        return new EmailRuntimeConfigException(EmailErrorCode.EMAIL_RUNTIME_CONFIG_FETCH_FAILED, Map.of(), cause);
    }

}
