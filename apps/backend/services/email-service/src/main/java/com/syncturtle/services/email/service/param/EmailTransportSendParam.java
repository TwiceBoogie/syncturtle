package com.syncturtle.services.email.service.param;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import com.syncturtle.services.email.service.collaborator.EmailRuntimeConfigSnapshot;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class EmailTransportSendParam {

    private final EmailRuntimeConfigSnapshot config;
    private final List<String> recipients;
    private final String subject;
    private final String textBody;
    private final String htmlBody;

    @Builder
    private EmailTransportSendParam(
            EmailRuntimeConfigSnapshot config,
            List<String> recipients,
            String subject,
            String textBody,
            String htmlBody) {
        Assert.notNull(config, "runtime config is required");
        Assert.state(config.isComplete(), "runtime config must be complete");

        this.config = config;
        this.recipients = normalizeRecipients(recipients);
        this.subject = normalizeRequired(subject, "subject is required");
        this.textBody = normalizeRequired(textBody, "textBody is required");
        this.htmlBody = normalizeOptional(htmlBody);
    }

    public boolean hasHtmlBody() {
        return htmlBody != null;
    }

    private static List<String> normalizeRecipients(List<String> recipients) {
        Assert.notNull(recipients, "recipients are required");
        Assert.notEmpty(recipients, "at least one recipient is required");

        List<String> normalized = new ArrayList<>(recipients.size());

        for (String recipient : recipients) {
            normalized.add(normalizeRequired(recipient, "recipient is required"));
        }

        return List.copyOf(normalized);
    }

    private static String normalizeRequired(String value, String message) {
        Assert.hasText(value, message);

        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

}
