package com.syncturtle.services.email.service.collaborator;

import static com.syncturtle.services.email.support.assertion.EmailExceptionAssert.assertThatEmailExceptionThrownBy;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.RECIPIENT;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.unauthenticatedRuntimeConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.services.email.configuration.property.EmailTransportProperties;
import com.syncturtle.services.email.service.param.EmailTransportSendParam;
import com.syncturtle.services.email.support.fixture.EmailFixtures.MagicCode;
import com.syncturtle.services.email.support.smtp.TestSmtpServer;

@DisplayName("EmailTransportSender")
class EmailTransportSenderIT {

    private final EmailTransportProperties properties = new EmailTransportProperties(Duration.ofSeconds(1),
            Duration.ofSeconds(1), Duration.ofSeconds(1));
    private final EmailTransportSender sender = new EmailTransportSender(properties);

    @Nested
    @DisplayName("send(EmailTransportSendParam)")
    class SendTests {

        @Test
        @DisplayName("sends MIME message through SMTP transport")
        void sendsMimeMessageThroughSmtpTransport() throws Exception {
            try (TestSmtpServer smtpServer = TestSmtpServer.start()) {
                EmailTransportSendParam param = EmailTransportSendParam.builder()
                        .config(unauthenticatedRuntimeConfig("127.0.0.1", smtpServer.getPort()))
                        .recipients(List.of(RECIPIENT))
                        .subject(MagicCode.SUBJECT)
                        .textBody(MagicCode.TEXT_BODY)
                        .build();

                sender.send(param);

                assertThat(smtpServer.awaitMessage(Duration.ofSeconds(2))).isTrue();
                assertThat(smtpServer.getMessageData())
                        .contains("Subject: " + MagicCode.SUBJECT)
                        .contains(MagicCode.TEXT_BODY);
            }
        }

        @Test
        @DisplayName("translates invalid from address")
        void translatesInvalidFromAddress() {
            EmailTransportSendParam param = EmailTransportSendParam.builder()
                    .config(EmailRuntimeConfigSnapshot.builder()
                            .enabled(true)
                            .host("127.0.0.1")
                            .port(2525)
                            .from("invalid address")
                            .version(1)
                            .build())
                    .recipients(List.of(RECIPIENT))
                    .subject(MagicCode.SUBJECT)
                    .textBody(MagicCode.TEXT_BODY)
                    .build();

            assertThatEmailExceptionThrownBy(() -> sender.send(param))
                    .hasErrorCode(EmailErrorCode.EMAIL_SMTP_INVALID_FROM_ADDRESS);
        }

    }

}
