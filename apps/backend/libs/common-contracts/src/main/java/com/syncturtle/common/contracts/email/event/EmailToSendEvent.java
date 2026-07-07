package com.syncturtle.common.contracts.email.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.common.contracts.messaging.OutboxEvent;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class EmailToSendEvent implements OutboxEvent {

    public static final String EVENT_TYPE = "EMAIL_TO_SEND";

    private final String eventId;
    private final Instant occurredAt;
    private final String correlationId;
    private final EmailTemplateType templateType;
    private final String subject;
    private final List<String> to;
    private final Map<String, Object> model;

    @Builder
    @Jacksonized
    private EmailToSendEvent(
            String eventId,
            Instant occurredAt,
            String correlationId,
            EmailTemplateType templateType,
            String subject,
            List<String> to,
            Map<String, Object> model) {
        this.eventId = requireText(eventId, "eventId is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.correlationId = requireText(correlationId, "correlationId is required");

        this.templateType = Objects.requireNonNull(templateType, "templateType is required");
        this.subject = requireText(subject, "subject is required");
        this.to = normalizeRecipients(to);
        this.model = normalizeModel(model);
    }

    @Override
    public String eventTypeName() {
        return EVENT_TYPE;
    }

    private static List<String> normalizeRecipients(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("to is required");
        }

        List<String> normalized = new ArrayList<>();

        for (String value : values) {
            normalized.add(requireText(value, "to recipient is required"));
        }

        return Collections.unmodifiableList(normalized);
    }

    private static Map<String, Object> normalizeModel(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> normalized = new LinkedHashMap<>();

        for (Entry<String, Object> entry : values.entrySet()) {
            String key = requireText(entry.getKey(), "model key is required");
            Object value = Objects.requireNonNull(entry.getValue(), "model values is required for key " + key);

            normalized.put(key, value);
        }

        return Collections.unmodifiableMap(normalized);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }
}
