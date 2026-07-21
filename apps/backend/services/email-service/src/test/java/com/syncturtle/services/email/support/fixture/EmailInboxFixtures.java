package com.syncturtle.services.email.support.fixture;

import java.time.Duration;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.configuration.property.EmailInboxProperties;
import com.syncturtle.services.email.support.clock.TestClocks;
import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.model.param.EmailEventInboxCreateParam;
import com.syncturtle.services.email.model.param.EmailEventInboxRetryFailureParam;

public final class EmailInboxFixtures {

    public static final String RECIPIENT_TO_JSON = "[\"lunasnow@marvel.com\",\"suestorm@marvel.com\"]";
    public static final String TEMPLATE_MODEL_JSON = "{\"displayName\":\"Luna\",\"magicLink\":\"https://example.com/magic\"}";
    public static final Duration PROCESSING_LEASE = Duration.ofHours(5);
    public static final Duration RETRY_DELAY = Duration.ofSeconds(30);
    public static final int MAX_ATTEMPTS = 3;
    public static final int BATCH_SIZE = 25;
    public static final String ERROR_MESSAGE = "ConnectException: connection refused";

    private EmailInboxFixtures() {
    }

    public static EmailEventInboxCreateParam createParam() {
        return EmailEventInboxCreateParam.builder()
                .eventId(EmailFixtures.EVENT_ID)
                .eventType(EmailFixtures.EVENT_TYPE)
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject(EmailFixtures.SUBJECT)
                .recipientToJson(RECIPIENT_TO_JSON)
                .templateModelJson(TEMPLATE_MODEL_JSON)
                .build();
    }

    public static EmailEventInbox processingInbox() {
        return EmailEventInbox.create(createParam(), TestClocks.fixedUtc(), PROCESSING_LEASE);
    }

    public static EmailEventInboxRetryFailureParam retryFailureParam() {
        return new EmailEventInboxRetryFailureParam(MAX_ATTEMPTS, RETRY_DELAY, ERROR_MESSAGE);
    }

    public static EmailInboxProperties inboxProperties() {
        return new EmailInboxProperties(new EmailInboxProperties.ProcessingProperties(PROCESSING_LEASE, BATCH_SIZE),
                new EmailInboxProperties.RetryProperties(MAX_ATTEMPTS, RETRY_DELAY, Duration.ofMinutes(2)));
    }

}
