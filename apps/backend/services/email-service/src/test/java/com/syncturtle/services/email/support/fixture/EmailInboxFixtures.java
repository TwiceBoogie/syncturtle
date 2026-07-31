package com.syncturtle.services.email.support.fixture;

import java.time.Duration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.configuration.property.EmailInboxProperties;
import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.model.param.EmailEventInboxCreateParam;
import com.syncturtle.services.email.model.param.EmailEventInboxRetryFailureParam;
import com.syncturtle.services.email.support.clock.TestClocks;

public final class EmailInboxFixtures {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    public static final String RECIPIENT_TO_JSON = writeJson(EmailFixtures.RECIPIENTS);
    public static final String TEMPLATE_MODEL_JSON = writeJson(EmailFixtures.MagicCode.MODEL);

    public static final Duration PROCESSING_LEASE = Duration.ofHours(5);
    public static final int BATCH_SIZE = 25;

    public static final int MAX_ATTEMPTS = 3;
    public static final Duration RETRY_DELAY = Duration.ofSeconds(30);
    public static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(2);

    public static final String ERROR_MESSAGE = "ConnectException: connection refused";

    private EmailInboxFixtures() {
        throw new AssertionError("EmailInboxFixtures must not be instantiated");
    }

    public static EmailEventInboxCreateParam magicCodeCreateParam() {
        return EmailEventInboxCreateParam.builder()
                .eventId(EmailFixtures.EVENT_ID)
                .eventType(EmailFixtures.EVENT_TYPE)
                .templateType(EmailTemplateType.MAGIC_CODE)
                .subject(EmailFixtures.MagicCode.SUBJECT)
                .recipientToJson(RECIPIENT_TO_JSON)
                .templateModelJson(TEMPLATE_MODEL_JSON)
                .build();
    }

    public static EmailEventInboxCreateParam magicCodeCreateParam(String eventId) {
        return EmailEventInboxCreateParam.builder()
                .eventId(eventId)
                .eventType(EmailFixtures.EVENT_TYPE)
                .templateType(EmailTemplateType.MAGIC_CODE)
                .subject(EmailFixtures.MagicCode.SUBJECT)
                .recipientToJson(RECIPIENT_TO_JSON)
                .templateModelJson(TEMPLATE_MODEL_JSON)
                .build();
    }

    public static EmailEventInbox processingMagicCodeInbox() {
        return EmailEventInbox.create(
                magicCodeCreateParam(),
                TestClocks.fixedUtc(),
                PROCESSING_LEASE);
    }

    public static EmailEventInboxRetryFailureParam retryFailureParam() {
        return new EmailEventInboxRetryFailureParam(
                MAX_ATTEMPTS,
                RETRY_DELAY,
                ERROR_MESSAGE);
    }

    public static EmailInboxProperties inboxProperties() {
        EmailInboxProperties.ProcessingProperties processingProperties = new EmailInboxProperties.ProcessingProperties(
                PROCESSING_LEASE,
                BATCH_SIZE);

        EmailInboxProperties.RetryProperties retryProperties = new EmailInboxProperties.RetryProperties(
                MAX_ATTEMPTS,
                RETRY_DELAY,
                MAX_RETRY_DELAY);
        EmailInboxProperties.SchedulerProperties schedulerProperties = new EmailInboxProperties.SchedulerProperties(
                false, Duration.ofSeconds(10));

        return new EmailInboxProperties(
                processingProperties,
                retryProperties,
                schedulerProperties);
    }

    private static String writeJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize email inbox fixture value",
                    exception);
        }
    }

}