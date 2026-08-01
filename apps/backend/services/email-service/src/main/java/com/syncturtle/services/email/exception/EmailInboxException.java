package com.syncturtle.services.email.exception;

import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.core.exception.SyncturtleServiceException;

public final class EmailInboxException extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;

    private EmailInboxException(EmailErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(errorCode, payload, cause);
        this.emailErrorCode = Objects.requireNonNull(errorCode, "errorCode is required");
    }

    public EmailErrorCode getEmailErrorCode() {
        return emailErrorCode;
    }

    public static EmailInboxException rowNotFound(String eventId) {
        return new EmailInboxException(EmailErrorCode.EMAIL_INBOX_ROW_NOT_FOUND, Map.of("event_id", eventId), null);
    }

    public static EmailInboxException payloadSerializationFailed(Throwable cause) {
        return new EmailInboxException(EmailErrorCode.EMAIL_INBOX_PAYLOAD_SERIALIZATION_FAILED, Map.of(), cause);
    }

    public static EmailInboxException payloadDeserializationFailed(String eventId, Throwable cause) {
        return new EmailInboxException(EmailErrorCode.EMAIL_INBOX_PAYLOAD_DESERIALIZATION_FAILED,
                Map.of("event_id", eventId), cause);
    }

}
