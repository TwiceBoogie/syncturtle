package com.syncturtle.services.email.exceptions;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.spring.web.error.SyncturtleServiceException;

public final class EmailInboxException extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;

    private EmailInboxException(
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

    public static EmailInboxException rowNotFound(String eventId) {
        return new EmailInboxException(
                EmailErrorCode.EMAIL_INBOX_ROW_NOT_FOUND,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Email inbox row was not found.",
                Map.of("eventId", eventId),
                null);
    }

    public static EmailInboxException payloadSerializationFailed(Throwable cause) {
        return new EmailInboxException(
                EmailErrorCode.EMAIL_INBOX_PAYLOAD_SERIALIZATION_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to serilaize email inbox payload.",
                Map.of(),
                cause);
    }

    public static EmailInboxException payloadDeserializationFailed(String eventId, Throwable cause) {
        return new EmailInboxException(
                EmailErrorCode.EMAIL_INBOX_PAYLOAD_DESERIALIZATION_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to deserilaize email inbox payload.",
                Map.of("event_id", eventId),
                cause);
    }

}
