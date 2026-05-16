package com.syncturtle.services.email.exceptions;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.spring.web.error.SyncturtleServiceException;

public final class EmailDispatchException extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;
    private final boolean retryable;

    private EmailDispatchException(
            EmailErrorCode errorCode,
            boolean retryable,
            HttpStatus status,
            String publicMessage,
            Map<String, Object> payload,
            Throwable cause) {
        super(errorCode, status, publicMessage, payload, cause);
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
        return retryable(EmailErrorCode.EMAIL_SMTP_DISABLED, "SMTP is disabled.", null);
    }

    public static EmailDispatchException smtpNotConfigured() {
        return retryable(
                EmailErrorCode.EMAIL_SMTP_NOT_CONFIGURED,
                "SMTP config is incomplete.",
                null);
    }

    public static EmailDispatchException authenticationFailed(Throwable cause) {
        return retryable(
                EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED,
                "SMTP authentication failed.",
                cause);
    }

    public static EmailDispatchException connectionFailed(Throwable cause) {
        return retryable(
                EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED,
                "Could not connect to the SMTP server.",
                cause);
    }

    public static EmailDispatchException timeout(Throwable cause) {
        return retryable(
                EmailErrorCode.EMAIL_SMTP_TIMEOUT,
                "Timed out while sending email.",
                cause);
    }

    public static EmailDispatchException sendFailed(Throwable cause) {
        return retryable(
                EmailErrorCode.EMAIL_DISPATCH_FAILED,
                "Failed to send email.",
                cause);
    }

    public static EmailDispatchException recipientsRefused(Throwable cause) {
        return permanent(
                EmailErrorCode.EMAIL_SMTP_RECIPIENTS_REFUSED,
                "All recipient addresses were refused.",
                cause);
    }

    public static EmailDispatchException messageBuildFailed(Throwable cause) {
        return permanent(
                EmailErrorCode.EMAIL_MESSAGE_BUILD_FAILED,
                "Failed to build email message.",
                cause);
    }

    public static EmailDispatchException templateFailed(Throwable cause) {
        return permanent(
                EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED,
                "Email template rendering failed.",
                cause);
    }

    private static EmailDispatchException retryable(
            EmailErrorCode errorCode,
            String publicMessage,
            Throwable cause) {
        return new EmailDispatchException(
                errorCode,
                true,
                HttpStatus.SERVICE_UNAVAILABLE,
                publicMessage,
                Map.of(),
                cause);
    }

    private static EmailDispatchException permanent(
            EmailErrorCode errorCode,
            String publicMessage,
            Throwable cause) {
        return new EmailDispatchException(
                errorCode,
                false,
                HttpStatus.BAD_REQUEST,
                publicMessage,
                Map.of(),
                cause);
    }

}
