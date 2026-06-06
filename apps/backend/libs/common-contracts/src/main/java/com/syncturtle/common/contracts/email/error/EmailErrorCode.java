package com.syncturtle.common.contracts.email.error;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.syncturtle.common.core.error.ErrorCode;

public enum EmailErrorCode implements ErrorCode {

    EMAIL_CREDENTIAL_CHECK_FAILED(
            7000,
            "EMAIL_CREDENTIAL_CHECK_FAILED",
            400,
            "Email credential check failed."),

    EMAIL_SMTP_NOT_CONFIGURED(
            7001,
            "EMAIL_SMTP_NOT_CONFIGURED",
            503,
            "Email delivery is not configured."),

    EMAIL_SMTP_AUTHENTICATION_FAILED(
            7002,
            "EMAIL_SMTP_AUTHENTICATION_FAILED",
            400,
            "SMTP authentication failed."),

    EMAIL_SMTP_CONNECTION_FAILED(
            7003,
            "EMAIL_SMTP_CONNECTION_FAILED",
            502,
            "Could not connect to the SMTP server."),

    EMAIL_SMTP_TIMEOUT(
            7004,
            "EMAIL_SMTP_TIMEOUT",
            504,
            "Timed out while communicating with the SMTP server."),

    EMAIL_SMTP_DISABLED(
            7005,
            "EMAIL_SMTP_DISABLED",
            403,
            "SMTP email delivery is disabled."),

    EMAIL_SMTP_INVALID_FROM_ADDRESS(
            7006,
            "EMAIL_SMTP_INVALID_FROM_ADDRESS",
            400,
            "From address is invalid."),

    EMAIL_SMTP_RECIPIENTS_REFUSED(
            7007,
            "EMAIL_SMTP_RECIPIENTS_REFUSED",
            400,
            "Recipient address was refused."),

    EMAIL_SMTP_SEND_FAILED(
            7008,
            "EMAIL_SMTP_SEND_FAILED",
            502,
            "Could not send email."),

    EMAIL_DISPATCH_FAILED(
            7010,
            "EMAIL_DISPATCH_FAILED",
            503,
            "Failed to dispatch email."),

    EMAIL_MESSAGE_BUILD_FAILED(
            7011,
            "EMAIL_MESSAGE_BUILD_FAILED",
            500,
            "Failed to build email message."),

    EMAIL_TEMPLATE_UNSUPPORTED(
            7012,
            "EMAIL_TEMPLATE_UNSUPPORTED",
            500,
            "Email template is not supported."),

    EMAIL_TEMPLATE_RENDER_FAILED(
            7013,
            "EMAIL_TEMPLATE_RENDER_FAILED",
            500,
            "Email template rendering failed."),

    EMAIL_ENVELOPE_INVALID(
            7014,
            "EMAIL_ENVELOPE_INVALID",
            400,
            "Email envelope is invalid."),

    EMAIL_RUNTIME_CONFIG_FETCH_FAILED(
            7020,
            "EMAIL_RUNTIME_CONFIG_FETCH_FAILED",
            503,
            "Failed to fetch email runtime configuration."),

    EMAIL_RUNTIME_CONFIG_STALE(
            7021,
            "EMAIL_RUNTIME_CONFIG_STALE",
            503,
            "Email runtime configuration is stale."),

    EMAIL_RUNTIME_CONFIG_INCOMPLETE(
            7022,
            "EMAIL_RUNTIME_CONFIG_INCOMPLETE",
            500,
            "Email runtime configuration is incomplete."),

    EMAIL_INBOX_ROW_NOT_FOUND(
            7030,
            "EMAIL_INBOX_ROW_NOT_FOUND",
            500,
            "Email inbox row was not found."),

    EMAIL_INBOX_PAYLOAD_SERIALIZATION_FAILED(
            7031,
            "EMAIL_INBOX_PAYLOAD_SERIALIZATION_FAILED",
            500,
            "Failed to serialize email inbox payload."),

    EMAIL_INBOX_PAYLOAD_DESERIALIZATION_FAILED(
            7032,
            "EMAIL_INBOX_PAYLOAD_DESERIALIZATION_FAILED",
            500,
            "Failed to deserialize email inbox payload."),

    EMAIL_OPERATION_FAILED(
            7099,
            "EMAIL_OPERATION_FAILED",
            500,
            "Email operation failed.");

    private static final Map<Integer, EmailErrorCode> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(EmailErrorCode::getCode, Function.identity()));

    private final int code;
    private final String key;
    private final int httpStatusCode;
    private final String publicMessage;

    EmailErrorCode(int code, String key, int httpStatusCode, String publicMessage) {
        this.code = code;
        this.key = key;
        this.httpStatusCode = httpStatusCode;
        this.publicMessage = publicMessage;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    @Override
    public String getPublicMessage() {
        return publicMessage;
    }

    public static EmailErrorCode fromCode(int code) {
        EmailErrorCode value = BY_CODE.get(code);

        if (value == null) {
            throw new IllegalArgumentException("Unknown EmailErrorCode: " + code);
        }

        return value;
    }

    public static EmailErrorCode fromCodeOrDefault(int code) {
        return BY_CODE.getOrDefault(code, EMAIL_OPERATION_FAILED);
    }

}