package com.syncturtle.services.email.exceptions;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.common.spring.web.error.SyncturtleServiceException;

public final class EmailTemplateException extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;

    private EmailTemplateException(
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

    public static EmailTemplateException invalidEnvelope(String reason) {
        return new EmailTemplateException(
                EmailErrorCode.EMAIL_ENVELOPE_INVALID,
                HttpStatus.BAD_REQUEST,
                "Email envelope is invalid.",
                Map.of("reason", reason),
                null);
    }

    public static EmailTemplateException unsupportedTemplate(EmailTemplateType templateType) {
        return new EmailTemplateException(
                EmailErrorCode.EMAIL_TEMPLATE_UNSUPPORTED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Email template is not supported.",
                Map.of("template_type", templateType.name()),
                null);
    }

    public static EmailTemplateException renderFailed(EmailTemplateType templateType, Throwable cause) {
        return new EmailTemplateException(
                EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Email template rendering failed.",
                Map.of("template_type",
                        templateType.name()),
                cause);
    }

}
