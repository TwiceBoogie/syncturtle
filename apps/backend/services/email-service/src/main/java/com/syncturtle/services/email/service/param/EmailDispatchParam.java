package com.syncturtle.services.email.service.param;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class EmailDispatchParam {

    private final String eventId;
    private final EmailTemplateType templateType;
    private final String subject;
    private final List<String> recipients;
    private final Map<String, Object> model;

    @Builder
    private EmailDispatchParam(
            String eventId,
            EmailTemplateType templateType,
            String subject,
            List<String> recipients,
            Map<String, Object> model) {
        Assert.notNull(templateType, "templateType is required");

        this.eventId = normalizeRequired(eventId, "eventId is required");
        this.templateType = templateType;
        this.subject = normalizeRequired(subject, "subject is required");
        this.recipients = normalizeRecipients(recipients);
        this.model = normalizeModel(model);
    }

    private static List<String> normalizeRecipients(List<String> recipients) {
        Assert.notNull(recipients, "recipients is required");
        Assert.notEmpty(recipients, "at least one recipient is required");

        List<String> normalized = new ArrayList<>(recipients.size());

        for (String recipient : recipients) {
            normalized.add(normalizeRequired(recipient, "recipient is required"));
        }

        return List.copyOf(normalized);
    }

    private static Map<String, Object> normalizeModel(Map<String, Object> model) {
        Assert.notNull(model, "model is required");

        Map<String, Object> normalized = new LinkedHashMap<>();

        for (Entry<String, Object> entry : model.entrySet()) {
            String key = normalizeRequired(entry.getKey(), "model key is required");

            Assert.notNull(entry.getValue(), "model value is required for key " + key);

            normalized.put(key, entry.getValue());
        }

        return Collections.unmodifiableMap(normalized);
    }

    private static String normalizeRequired(String value, String message) {
        Assert.hasText(value, message);

        return value.trim();
    }

}
