package com.syncturtle.services.email.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.dto.EmailEnvelope;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    @Qualifier("emailTemplateEngine")
    private final TemplateEngine emailTemplateEngine;

    public RenderedEmail render(EmailEnvelope envelope) {
        validateEnvelope(envelope);

        TemplateSet templateSet = resolveTemplateSet(envelope.getTemplateType());
        Context context = createContext(envelope.getModel());

        try {
            String html = emailTemplateEngine.process(templateSet.htmlTemplate(), context);
            String text = emailTemplateEngine.process(templateSet.textTemplate(), context);

            return new RenderedEmail(normalizeSubject(envelope.getSubject()), html, text);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed rendering template. type=" + envelope.getTemplateType(), exception);
        }
    }

    private void validateEnvelope(EmailEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        Objects.requireNonNull(envelope.getTemplateType(), "templateType must not be null");

        if (!StringUtils.hasText(envelope.getSubject())) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        if (envelope.getTo() == null || envelope.getTo().isEmpty()) {
            throw new IllegalArgumentException("at least one recipient is required");
        }
    }

    private TemplateSet resolveTemplateSet(EmailTemplateType templateType) {
        return switch (templateType) {
            case MAGIC_LINK -> new TemplateSet("email/html/magic-link", "email/text/magic-link");
            case PASSWORD_RESET -> throw new UnsupportedOperationException("PASSWORD_RESET not implemented yet");
            case VERIFY_EMAIL -> throw new UnsupportedOperationException("VERIFY_EMAIL not implemented yet");
            case GENERIC_HTML -> throw new UnsupportedOperationException("GENERIC_HTML not implemented yet");
        };
    }

    private Context createContext(Map<String, Object> model) {
        Map<String, Object> safeModel = new LinkedHashMap<>();
        if (model != null) {
            safeModel.putAll(model);
        }

        Context context = new Context();
        context.setVariables(safeModel);
        return context;
    }

    private String normalizeSubject(String subject) {
        return subject.trim();
    }

    private record TemplateSet(String htmlTemplate, String textTemplate) {
    }

    public record RenderedEmail(String subject, String htmlBody, String textBody) {
    }

}
