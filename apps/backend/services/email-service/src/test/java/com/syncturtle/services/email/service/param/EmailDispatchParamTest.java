package com.syncturtle.services.email.service.param;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.support.fixture.EmailFixtures.MagicCode;

@DisplayName("EmailDispatchParam")
class EmailDispatchParamTest {

    @Test
    @DisplayName("normalizes and defensively copies dispatch data")
    void normalizesAndDefensivelyCopiesDispatchData() {
        // arrange
        List<String> recipients = new ArrayList<>(List.of(" lunasnow@marvel.com "));
        Map<String, Object> model = new LinkedHashMap<>(MagicCode.MODEL);
        EmailDispatchParam param = EmailDispatchParam.builder()
                .eventId(" " + EVENT_ID + " ")
                .templateType(EmailTemplateType.MAGIC_CODE)
                .subject(" " + MagicCode.SUBJECT + " ")
                .recipients(recipients)
                .model(model)
                .build();
        recipients.clear();
        model.clear();
        // conditions
        // act
        // assert
        assertThat(param.getEventId()).isEqualTo(EVENT_ID);
        assertThat(param.getSubject()).isEqualTo(MagicCode.SUBJECT);
        assertThat(param.getRecipients()).containsExactly("lunasnow@marvel.com");
        assertThat(param.getModel()).containsAllEntriesOf(MagicCode.MODEL);
        assertThatThrownBy(() -> param.getModel().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("rejects empty recipients")
    void rejectsEmptyRecipients() {
        // arrange
        // conditions
        // act
        // assert
        assertThatThrownBy(() -> EmailDispatchParam.builder()
                .eventId(EVENT_ID)
                .templateType(EmailTemplateType.MAGIC_CODE)
                .subject(MagicCode.SUBJECT)
                .recipients(List.of())
                .model(Map.of())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("at least one recipient is required");
    }

    @Test
    @DisplayName("rejects null model value")
    void rejectsNullModelValue() {
        Map<String, Object> model = new LinkedHashMap<>();
        model.put("magicLink", null);

        assertThatThrownBy(() -> EmailDispatchParam.builder()
                .eventId(EVENT_ID)
                .templateType(EmailTemplateType.MAGIC_CODE)
                .subject(MagicCode.SUBJECT)
                .recipients(List.of("lunasnow@marvel.com"))
                .model(model)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("model value is required for key magicLink");
    }

}
