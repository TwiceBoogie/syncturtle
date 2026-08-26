package com.syncturtle.services.email.service.result;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class RenderedEmailResult {

    private final String subject;
    private final String htmlBody;
    private final String textBody;

    public RenderedEmailResult(String subject, String htmlBody, String textBody) {
        Assert.hasText(subject, "subject is required");
        Assert.hasText(htmlBody, "htmlBody is required");
        Assert.hasText(textBody, "textBody is required");

        this.subject = subject.trim();
        this.htmlBody = htmlBody;
        this.textBody = textBody;
    }

}
