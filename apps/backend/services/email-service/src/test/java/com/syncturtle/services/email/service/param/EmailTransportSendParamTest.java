package com.syncturtle.services.email.service.param;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.RECIPIENTS;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.completeRuntimeConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures;
import com.syncturtle.services.email.support.fixture.EmailFixtures.MagicCode;

@DisplayName("EmailTransportSendParam")
class EmailTransportSendParamTest {

    @Nested
    @DisplayName("hasHtmlBody()")
    class HasHtmlBodyTests {

        @Test
        @DisplayName("returns true for normalized HTML content")
        void returnsTrueForNormalizedHtmlContent() {
            EmailTransportSendParam param = EmailTransportSendParam.builder()
                    .config(completeRuntimeConfig())
                    .recipients(RECIPIENTS)
                    .subject(MagicCode.SUBJECT)
                    .textBody(MagicCode.TEXT_BODY)
                    .htmlBody(MagicCode.HTML_BODY)
                    .build();

            assertThat(param.hasHtmlBody()).isTrue();
        }

        @Test
        @DisplayName("returns false for blank HTML content")
        void returnsFalseForBlankHtmlContent() {
            EmailTransportSendParam param = EmailTransportSendParam.builder()
                    .config(completeRuntimeConfig())
                    .recipients(RECIPIENTS)
                    .subject(MagicCode.SUBJECT)
                    .textBody(MagicCode.TEXT_BODY)
                    .htmlBody(" ")
                    .build();

            assertThat(param.hasHtmlBody()).isFalse();
            assertThat(param.getHtmlBody()).isNull();
        }
    }

    @Test
    @DisplayName("defensively copies and normalizes recipients")
    void defensivelyCopiesAndNormalizesRecipients() {
        List<String> recipients = new ArrayList<>(List.of("  lunasnow@marvel.com  "));

        EmailTransportSendParam param = EmailTransportSendParam.builder()
                .config(completeRuntimeConfig())
                .recipients(recipients)
                .subject(MagicCode.SUBJECT)
                .textBody(MagicCode.TEXT_BODY)
                .build();
        recipients.clear();

        assertThat(param.getRecipients()).containsExactly("lunasnow@marvel.com");
        assertThatThrownBy(() -> param.getRecipients().add("suestorm@marvel.com"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("rejects incomplete runtime configuration")
    void rejectsIncompleteRuntimeConfiguration() {
        assertThatThrownBy(() -> EmailTransportSendParam.builder()
                .config(EmailRuntimeConfigFixtures.incompleteRuntimeConfig())
                .recipients(RECIPIENTS)
                .subject(MagicCode.SUBJECT)
                .textBody(MagicCode.TEXT_BODY)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("runtime config must be complete");
    }

}
