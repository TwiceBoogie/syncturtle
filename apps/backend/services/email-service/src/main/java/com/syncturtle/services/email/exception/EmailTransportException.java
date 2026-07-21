package com.syncturtle.services.email.exception;

import java.util.Map;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.core.exceptions.SyncturtleServiceException;

public final class EmailTransportException
        extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;

    private EmailTransportException(EmailErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(requireErrorCode(errorCode), payload, cause);
        this.emailErrorCode = errorCode;
    }

    public EmailErrorCode getEmailErrorCode() {
        return emailErrorCode;
    }

    public EmailTransportException with(String key, Object value) {
        Assert.hasText(key, "payload key is required");

        return new EmailTransportException(emailErrorCode, copyPayloadWith(key.trim(), value), getCause());
    }

    public static EmailTransportException authenticationFailed(Throwable cause) {
        return new EmailTransportException(EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED, Map.of(),
                requireCause(cause));
    }

    public static EmailTransportException connectionFailed(Throwable cause) {
        return new EmailTransportException(EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED, Map.of(), requireCause(cause));
    }

    public static EmailTransportException timeout(Throwable cause) {
        return new EmailTransportException(EmailErrorCode.EMAIL_SMTP_TIMEOUT, Map.of(), requireCause(cause));
    }

    public static EmailTransportException invalidFromAddress(Throwable cause) {
        return new EmailTransportException(EmailErrorCode.EMAIL_SMTP_INVALID_FROM_ADDRESS, Map.of(),
                requireCause(cause));
    }

    public static EmailTransportException recipientsRefused(Throwable cause) {
        return new EmailTransportException(EmailErrorCode.EMAIL_SMTP_RECIPIENTS_REFUSED, Map.of(), requireCause(cause));
    }

    public static EmailTransportException messageBuildFailed(Throwable cause) {
        return new EmailTransportException(EmailErrorCode.EMAIL_MESSAGE_BUILD_FAILED, Map.of(), requireCause(cause));
    }

    public static EmailTransportException sendFailed(Throwable cause) {
        return new EmailTransportException(EmailErrorCode.EMAIL_SMTP_SEND_FAILED, Map.of(), requireCause(cause));
    }

    private static EmailErrorCode requireErrorCode(EmailErrorCode errorCode) {
        Assert.notNull(errorCode, "errorCode is required");

        return errorCode;
    }

    private static Throwable requireCause(Throwable cause) {
        Assert.notNull(cause, "cause is required");

        return cause;
    }
}