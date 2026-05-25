package com.syncturtle.common.contracts.email.error;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.syncturtle.common.core.error.ErrorCode;

public enum EmailErrorCode implements ErrorCode {

    EMAIL_CREDENTIAL_CHECK_FAILED(7000, "EMAIL_CREDENTIAL_CHECK_FAILED"),
    EMAIL_SMTP_NOT_CONFIGURED(7001, "EMAIL_SMTP_NOT_CONFIGURED"),
    EMAIL_SMTP_AUTHENTICATION_FAILED(7002, "EMAIL_SMTP_AUTHENTICATION_FAILED"),
    EMAIL_SMTP_CONNECTION_FAILED(7003, "EMAIL_SMTP_CONNECTION_FAILED"),
    EMAIL_SMTP_TIMEOUT(7004, "EMAIL_SMTP_TIMEOUT"),
    EMAIL_SMTP_DISABLED(7005, "EMAIL_SMTP_DISABLED"),
    EMAIL_SMTP_INVALID_FROM_ADDRESS(7006, "EMAIL_SMTP_INVALID_FROM_ADDRESS"),
    EMAIL_SMTP_RECIPIENTS_REFUSED(7007, "EMAIL_SMTP_RECIPIENTS_REFUSED"),
    EMAIL_SMTP_SEND_FAILED(7008, "EMAIL_SMTP_SEND_FAILED"),

    EMAIL_DISPATCH_FAILED(7010, "EMAIL_DISPATCH_FAILED"),
    EMAIL_MESSAGE_BUILD_FAILED(7011, "EMAIL_MESSAGE_BUILD_FAILED"),
    EMAIL_TEMPLATE_UNSUPPORTED(7012, "EMAIL_TEMPLATE_UNSUPPORTED"),
    EMAIL_TEMPLATE_RENDER_FAILED(7013, "EMAIL_TEMPLATE_RENDER_FAILED"),
    EMAIL_ENVELOPE_INVALID(7014, "EMAIL_ENVELOPE_INVALID"),

    EMAIL_RUNTIME_CONFIG_FETCH_FAILED(7020, "EMAIL_RUNTIME_CONFIG_FETCH_FAILED"),
    EMAIL_RUNTIME_CONFIG_STALE(7021, "EMAIL_RUNTIME_CONFIG_STALE"),
    EMAIL_RUNTIME_CONFIG_INCOMPLETE(7022, "EMAIL_RUNTIME_CONFIG_INCOMPLETE"),

    EMAIL_INBOX_ROW_NOT_FOUND(7030, "EMAIL_INBOX_ROW_NOT_FOUND"),
    EMAIL_INBOX_PAYLOAD_SERIALIZATION_FAILED(7031, "EMAIL_INBOX_PAYLOAD_SERIALIZATION_FAILED"),
    EMAIL_INBOX_PAYLOAD_DESERIALIZATION_FAILED(7032, "EMAIL_INBOX_PAYLOAD_DESERIALIZATION_FAILED");

    private static final Map<Integer, EmailErrorCode> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(EmailErrorCode::getCode, Function.identity()));

    private final int code;
    private final String key;

    EmailErrorCode(int code, String key) {
        this.code = code;
        this.key = key;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getKey() {
        return key;
    }

    public static EmailErrorCode fromCodeOrDefault(int code) {
        return BY_CODE.getOrDefault(code, EMAIL_CREDENTIAL_CHECK_FAILED);
    }

}
