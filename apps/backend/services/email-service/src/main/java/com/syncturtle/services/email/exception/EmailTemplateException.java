package com.syncturtle.services.email.exception;

import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.common.core.exceptions.SyncturtleServiceException;

public final class EmailTemplateException extends SyncturtleServiceException {

    private final EmailErrorCode emailErrorCode;

    private EmailTemplateException(
            EmailErrorCode errorCode,
            Map<String, Object> payload,
            Throwable cause) {
        super(errorCode, payload, cause);
        this.emailErrorCode = Objects.requireNonNull(errorCode, "errorCode is required");
    }

    public EmailErrorCode getEmailErrorCode() {
        return emailErrorCode;
    }

    public static EmailTemplateException invalidEnvelope(String reason) {
        return new EmailTemplateException(EmailErrorCode.EMAIL_ENVELOPE_INVALID, Map.of("reason", reason), null);
    }

    public static EmailTemplateException unsupportedTemplate(EmailTemplateType templateType) {
        return new EmailTemplateException(EmailErrorCode.EMAIL_TEMPLATE_UNSUPPORTED,
                Map.of("template_type", templateType.name()), null);
    }

    public static EmailTemplateException renderFailed(EmailTemplateType templateType, Throwable cause) {
        return new EmailTemplateException(
                EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED,
                Map.of("template_type", templateType.name()),
                cause);
    }

}
