package com.syncturtle.services.email.model.param;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_TYPE;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.SUBJECT;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.RECIPIENT_TO_JSON;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.TEMPLATE_MODEL_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;

@DisplayName("EmailEventInboxCreateParam")
class EmailEventInboxCreateParamTest {

    @Test
    @DisplayName("normalizes required text and preserves snapshot values")
    void normalizesRequiredTextAndPreservesSnapshotValues() {
        EmailEventInboxCreateParam param = EmailEventInboxCreateParam.builder()
                .eventId("  " + EVENT_ID + "  ")
                .eventType("  " + EVENT_TYPE + "  ")
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject("  " + SUBJECT + "  ")
                .recipientToJson(RECIPIENT_TO_JSON)
                .templateModelJson(TEMPLATE_MODEL_JSON)
                .build();

        assertThat(param.getEventId()).isEqualTo(EVENT_ID);
        assertThat(param.getEventType()).isEqualTo(EVENT_TYPE);
        assertThat(param.getSubject()).isEqualTo(SUBJECT);
        assertThat(param.getTemplateType()).isEqualTo(EmailTemplateType.MAGIC_LINK);
    }

    @Test
    @DisplayName("rejects event ID longer than database column")
    void rejectsEventIdLongerThanDatabaseColumn() {
        assertThatThrownBy(() -> EmailEventInboxCreateParam.builder()
                .eventId("x".repeat(101))
                .eventType(EVENT_TYPE)
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject(SUBJECT)
                .recipientToJson(RECIPIENT_TO_JSON)
                .templateModelJson(TEMPLATE_MODEL_JSON)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("eventId must be 100 characters or fewer");
    }

    @Test
    @DisplayName("rejects missing template type")
    void rejectsMissingTemplateType() {
        assertThatThrownBy(() -> EmailEventInboxCreateParam.builder()
                .eventId(EVENT_ID)
                .eventType(EVENT_TYPE)
                .subject(SUBJECT)
                .recipientToJson(RECIPIENT_TO_JSON)
                .templateModelJson(TEMPLATE_MODEL_JSON)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("templateType is required");
    }
}
