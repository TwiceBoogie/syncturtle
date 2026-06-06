package com.syncturtle.services.email.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.core.exceptions.SyncturtleServiceException;

public final class EmailCredentialCheckException extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;

    private EmailCredentialCheckException(EmailErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(errorCode, payload, cause);
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

        return new EmailCredentialCheckException(emailErrorCode, copy, getCause());
    }

    public static EmailCredentialCheckException credentialCheckFailed(Throwable cause) {
        return new EmailCredentialCheckException(EmailErrorCode.EMAIL_CREDENTIAL_CHECK_FAILED, Map.of(), cause);
    }

    public static EmailCredentialCheckException smtpDisabled() {
        return new EmailCredentialCheckException(EmailErrorCode.EMAIL_SMTP_DISABLED, Map.of(), null);
    }

    public static EmailCredentialCheckException smtpNotConfigured() {
        return new EmailCredentialCheckException(EmailErrorCode.EMAIL_SMTP_NOT_CONFIGURED, Map.of(), null);
    }

    public static EmailCredentialCheckException authenticationFailed(Throwable cause) {
        return new EmailCredentialCheckException(EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED, Map.of(), cause);
    }

    public static EmailCredentialCheckException connectionFailed(Throwable cause) {
        return new EmailCredentialCheckException(EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED, Map.of(), cause);
    }

    public static EmailCredentialCheckException timeout(Throwable cause) {
        return new EmailCredentialCheckException(EmailErrorCode.EMAIL_SMTP_TIMEOUT, Map.of(), cause);
    }

    public static EmailCredentialCheckException invalidFromAddress(Throwable cause) {
        return new EmailCredentialCheckException(EmailErrorCode.EMAIL_SMTP_INVALID_FROM_ADDRESS, Map.of(), cause);
    }

    public static EmailCredentialCheckException recipientsRefused(Throwable cause) {
        return new EmailCredentialCheckException(EmailErrorCode.EMAIL_SMTP_RECIPIENTS_REFUSED, Map.of(), cause);
    }

    public static EmailCredentialCheckException sendFailed(Throwable cause) {
        return new EmailCredentialCheckException(EmailErrorCode.EMAIL_SMTP_SEND_FAILED, Map.of(), cause);
    }

}
