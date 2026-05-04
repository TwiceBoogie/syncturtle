package com.syncturtle.services.email.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.core.exceptions.SyncturtleException;
import com.syncturtle.common.spring.web.error.PublicMessageAwareException;

public class EmailCredentialCheckException extends SyncturtleException implements PublicMessageAwareException {

    private final EmailErrorCode emailErrorCode;
    private final String publicMessage;

    public EmailCredentialCheckException(
            EmailErrorCode errorCode,
            String publicMessage,
            Map<String, Object> payload,
            Throwable cause) {
        super(errorCode, payload, cause);
        this.emailErrorCode = Objects.requireNonNull(errorCode, "errorCode is required");
        this.publicMessage = Objects.requireNonNull(publicMessage, "publicMessage is required");
    }

    public EmailErrorCode getEmailErrorCode() {
        return emailErrorCode;
    }

    @Override
    public String getPublicMessage() {
        return publicMessage;
    }

    public EmailCredentialCheckException with(String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(getPayload());

        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }

        return new EmailCredentialCheckException(emailErrorCode, publicMessage, copy, getCause());
    }

    public static EmailCredentialCheckException smtpDisabled() {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_DISABLED,
                "SMTP is disabled",
                Map.of(),
                null);
    }

    public static EmailCredentialCheckException smtpNotConfigured() {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_NOT_CONFIGURED,
                "Could not send email. Please check your configuration.",
                Map.of(),
                null);
    }

    public static EmailCredentialCheckException authenticationFailed(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED,
                "SMTP authentication failed",
                Map.of(),
                cause);
    }

    public static EmailCredentialCheckException connectionFailed(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED,
                "Could not connect to the SMTP server",
                Map.of(),
                cause);
    }

    public static EmailCredentialCheckException timeout(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_TIMEOUT,
                "Timed out while checking SMTP credentials",
                Map.of(),
                cause);
    }

    public static EmailCredentialCheckException invalidFromAddress(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_INVALID_FROM_ADDRESS,
                "From address is invalid",
                Map.of(),
                cause);
    }

    public static EmailCredentialCheckException recipientsRefused(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_RECIPIENTS_REFUSED,
                "All recipient addresses were refused",
                Map.of(),
                cause);
    }

    public static EmailCredentialCheckException sendFailed(Throwable cause) {
        return new EmailCredentialCheckException(
                EmailErrorCode.EMAIL_SMTP_SEND_FAILED,
                "Could not send email. Please check your configuration.",
                Map.of(),
                cause);
    }

}
