package com.syncturtle.services.email.service.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.email.support.fixture.EmailFixtures.MagicCode;

@DisplayName("RenderedEmailResult")
class RenderedEmailResultTest {

    @Test
    @DisplayName("normalizes subject and preserves rendered bodies")
    void normalizesSubjectAndPreservesRenderedBodies() {
        RenderedEmailResult result = new RenderedEmailResult("  " + MagicCode.SUBJECT + "  ", MagicCode.HTML_BODY,
                MagicCode.TEXT_BODY);

        assertThat(result.getSubject()).isEqualTo(MagicCode.SUBJECT);
        assertThat(result.getHtmlBody()).isEqualTo(MagicCode.HTML_BODY);
        assertThat(result.getTextBody()).isEqualTo(MagicCode.TEXT_BODY);
    }

    @Test
    @DisplayName("rejects blank rendered body")
    void rejectsBlankRenderedBody() {
        assertThatThrownBy(() -> new RenderedEmailResult(MagicCode.SUBJECT, " ", MagicCode.TEXT_BODY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("htmlBody is required");
    }
}
