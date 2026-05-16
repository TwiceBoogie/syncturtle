package com.syncturtle.services.email.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.spring.web.error.SyncturtleServiceException;

public final class EmailCredentialCheckException extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;

    public EmailCredentialCheckException(
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

    public EmailCredentialCheckException with(String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(getPayload());

        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }

        return new EmailCredentialCheckException(
                emailErrorCode,
                getStatus(),
                getPublicMessage(),
                copy,
                getCause());
    }

    public static EmailCredentialCheckException smtpDisabled() {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_DISABLED,
                HttpStatus.BAD_REQUEST,
                "SMTP is disabled.",
                Map.of(),
                null);
    }

    public static EmailCredentialCheckException smtpNotConfigured() {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_NOT_CONFIGURED,
                HttpStatus.BAD_REQUEST,
                "Could not send email. Please check your configuration.",
                Map.of(),
                null);
    }

    public static EmailCredentialCheckException authenticationFailed(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED,
                HttpStatus.BAD_REQUEST,
                "SMTP authentication failed.",
                Map.of(),
                cause);
    }

    public static EmailCredentialCheckException connectionFailed(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED,
                HttpStatus.BAD_GATEWAY,
                "Could not connect to the SMTP server.",
                Map.of(),
                cause);
    }

    public static EmailCredentialCheckException timeout(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_TIMEOUT,
                HttpStatus.GATEWAY_TIMEOUT,
                "Timed out while checking SMTP credentials.",
                Map.of(),
                cause);
    }

    public static EmailCredentialCheckException invalidFromAddress(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_INVALID_FROM_ADDRESS,
                HttpStatus.BAD_REQUEST,
                "From address is invalid.",
                Map.of(),
                cause);
    }

    public static EmailCredentialCheckException recipientsRefused(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_RECIPIENTS_REFUSED,
                HttpStatus.BAD_REQUEST,
                "All recipient addresses were refused.",
                Map.of(),
                cause);
    }

    public static EmailCredentialCheckException sendFailed(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_SEND_FAILED,
                HttpStatus.BAD_GATEWAY,
                "Could not send email. Please check your configuration.",
                Map.of(),
                cause);
    }

}
