package com.syncturtle.services.email.model.param;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class EmailEventInboxCreateParam {

    private static final int MAX_EVENT_ID_LENGTH = 100;
    private static final int MAX_EVENT_TYPE_LENGTH = 100;
    private static final int MAX_SUBJECT_LENGTH = 255;

    private final String eventId;
    private final String eventType;
    private final EmailTemplateType templateType;
    private final String subject;
    private final String recipientToJson;
    private final String templateModelJson;

    @Builder
    private EmailEventInboxCreateParam(
            String eventId,
            String eventType,
            EmailTemplateType templateType,
            String subject,
            String recipientToJson,
            String templateModelJson) {
        Assert.notNull(templateType, "templateType is required");
        Assert.hasText(recipientToJson, "recipientToJson is required");
        Assert.hasText(templateModelJson, "templateModelJson is required");

        this.eventId = normalizeRequired(eventId, "eventId", MAX_EVENT_ID_LENGTH);
        this.eventType = normalizeRequired(eventType, "eventType", MAX_EVENT_TYPE_LENGTH);
        this.templateType = templateType;
        this.subject = normalizeRequired(subject, "subject", MAX_SUBJECT_LENGTH);
        this.recipientToJson = recipientToJson;
        this.templateModelJson = templateModelJson;
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

}
