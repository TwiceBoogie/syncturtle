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
    EMAIL_SMTP_SEND_FAILED(7008, "EMAIL_SMTP_SEND_FAILED");

    private static final Map<Integer, EmailErrorCode> BY_CODE = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(EmailErrorCode::getCode, Function.identity()));

    private final int code;
    private final String message;

    EmailErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static EmailErrorCode fromCodeOrDefault(int code) {
        return BY_CODE.getOrDefault(code, EMAIL_CREDENTIAL_CHECK_FAILED);
    }

}
