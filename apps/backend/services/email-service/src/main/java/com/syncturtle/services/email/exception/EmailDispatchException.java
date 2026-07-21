package com.syncturtle.services.email.exception;

import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.core.exceptions.SyncturtleServiceException;

public final class EmailDispatchException extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;
    private final boolean retryable;

    private EmailDispatchException(
            EmailErrorCode errorCode,
            boolean retryable,
            Map<String, Object> payload,
            Throwable cause) {
        super(errorCode, payload, cause);
        this.emailErrorCode = Objects.requireNonNull(errorCode, "errorCode is required");
        this.retryable = retryable;
    }

    public EmailErrorCode getEmailErrorCode() {
        return emailErrorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public static EmailDispatchException smtpDisabled() {
        return retryable(EmailErrorCode.EMAIL_SMTP_DISABLED, null);
    }

    public static EmailDispatchException smtpNotConfigured() {
        return retryable(EmailErrorCode.EMAIL_SMTP_NOT_CONFIGURED, null);
    }

    public static EmailDispatchException authenticationFailed(Throwable cause) {
        return retryable(EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED, cause);
    }

    public static EmailDispatchException connectionFailed(Throwable cause) {
        return retryable(EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED, cause);
    }

    public static EmailDispatchException timeout(Throwable cause) {
        return retryable(EmailErrorCode.EMAIL_SMTP_TIMEOUT, cause);
    }

    public static EmailDispatchException sendFailed(Throwable cause) {
        return retryable(EmailErrorCode.EMAIL_DISPATCH_FAILED, cause);
    }

    public static EmailDispatchException dispatchFailed() {
        return retryable(EmailErrorCode.EMAIL_DISPATCH_FAILED, null);
    }

    public static EmailDispatchException recipientsRefused(Throwable cause) {
        return permanent(EmailErrorCode.EMAIL_SMTP_RECIPIENTS_REFUSED, cause);
    }

    public static EmailDispatchException messageBuildFailed(Throwable cause) {
        return permanent(EmailErrorCode.EMAIL_MESSAGE_BUILD_FAILED, cause);
    }

    public static EmailDispatchException templateFailed(Throwable cause) {
        return permanent(EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED, cause);
    }

    private static EmailDispatchException retryable(EmailErrorCode errorCode, Throwable cause) {
        return new EmailDispatchException(errorCode, true, Map.of("retryable", true), cause);
    }

    private static EmailDispatchException permanent(EmailErrorCode errorCode, Throwable cause) {
        return new EmailDispatchException(errorCode, false, Map.of("retryable", false), cause);
    }

}
