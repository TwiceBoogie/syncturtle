package com.syncturtle.services.email.support.fixture;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.service.param.EmailDispatchParam;
import com.syncturtle.services.email.service.result.RenderedEmailResult;

public final class EmailFixtures {

    public static final String EVENT_ID = "email-event-1001";
    public static final String EVENT_TYPE = EmailToSendEvent.EVENT_TYPE;
    public static final Instant OCCURRED_AT = Instant.parse("2026-07-19T13:59:00Z");
    public static final String RECIPIENT = "lunasnow@marvel.com";
    public static final String SECOND_RECIPIENT = "suestorm@marvel.com";
    public static final List<String> RECIPIENTS = List.of(RECIPIENT, SECOND_RECIPIENT);
    public static final String SUBJECT = "Your Syncturtle magic link";
    public static final String TEXT_BODY = "Use this link: https://app.syncturtle.com/magic";
    public static final String HTML_BODY = "<p>Use this link: <a href=\"https://app.syncturtle.com/magic\">Sign in</a></p>";
    public static final Map<String, Object> MODEL = Map.of(
            "displayName", "Snow",
            "magicLink", "https://app.syncturtle.com/magic");

    private EmailFixtures() {
    }

    public static EmailToSendEvent emailEvent() {
        return EmailToSendEvent.builder()
                .eventId(EVENT_ID)
                .occurredAt(OCCURRED_AT)
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject(SUBJECT)
                .to(RECIPIENTS)
                .model(MODEL)
                .build();
    }

    public static EmailDispatchParam dispatchParam() {
        return EmailDispatchParam.builder()
                .eventId(EVENT_ID)
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject(SUBJECT)
                .recipients(RECIPIENTS)
                .model(MODEL)
                .build();
    }

    public static RenderedEmailResult renderedEmail() {
        return new RenderedEmailResult(SUBJECT, HTML_BODY, TEXT_BODY);
    }
}
