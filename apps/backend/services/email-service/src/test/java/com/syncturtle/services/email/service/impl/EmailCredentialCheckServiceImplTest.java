package com.syncturtle.services.email.service.impl;

import static com.syncturtle.services.email.support.assertion.EmailExceptionAssert.assertThatEmailExceptionThrownBy;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.RECIPIENT;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.completeRuntimeConfig;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.disabledRuntimeConfig;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.incompleteRuntimeConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.services.email.exception.EmailTransportException;
import com.syncturtle.services.email.service.EmailCredentialCheckService;
import com.syncturtle.services.email.service.collaborator.EmailRuntimeConfigResolver;
import com.syncturtle.services.email.service.collaborator.EmailTransportSender;
import com.syncturtle.services.email.service.param.EmailTransportSendParam;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailCredentialCheckService")
class EmailCredentialCheckServiceImplTest {

    @Mock
    EmailRuntimeConfigResolver runtimeConfigResolver;
    @Mock
    EmailTransportSender transportSender;
    @Captor
    ArgumentCaptor<EmailTransportSendParam> sendParamCaptor;

    private EmailCredentialCheckService service;

    @BeforeEach
    void setup() {
        service = new EmailCredentialCheckServiceImpl(runtimeConfigResolver, transportSender);
    }

    @Nested
    @DisplayName("sendTestEmail(String)")
    class SendTestEmailTests {

        @Test
        @DisplayName("normalize recipient and sends plain text credentail check")
        void normalizeRecipientAndSendsPlainTextCredentialCheck() {
            // arrange
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(completeRuntimeConfig());
            // act
            service.sendTestEmail(" " + RECIPIENT + " ");
            // capture + verify
            verify(transportSender).send(sendParamCaptor.capture());
            EmailTransportSendParam param = sendParamCaptor.getValue();
            // assert
            assertThat(param.getRecipients()).containsExactly(RECIPIENT);
            assertThat(param.hasHtmlBody()).isFalse();
        }

        @Test
        @DisplayName("rejects blank recipient before resolving configuration")
        void rejectsBlankRecipientBeforeResolvingConfiguration() {
            // arrange
            // conditions
            // act
            assertThatThrownBy(() -> service.sendTestEmail(" "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("receiverEmail is required");
            // assert
            // verify
            verifyNoInteractions(runtimeConfigResolver, transportSender);
        }

        @Test
        @DisplayName("throws SMTP disabled")
        void throwsSmtpDisabled() {
            // arrange
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(disabledRuntimeConfig());
            // act + assert
            assertThatEmailExceptionThrownBy(() -> service.sendTestEmail(RECIPIENT))
                    .hasErrorCode(EmailErrorCode.EMAIL_SMTP_DISABLED);
            // verify
            verifyNoInteractions(transportSender);
        }

        @Test
        @DisplayName("throws SMTP not configured")
        void throwsSmtpNotConfigured() {
            // arrange
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(incompleteRuntimeConfig());
            // act + assert
            assertThatEmailExceptionThrownBy(() -> service.sendTestEmail(RECIPIENT))
                    .hasErrorCode(EmailErrorCode.EMAIL_SMTP_NOT_CONFIGURED);
            // verify
            verifyNoInteractions(transportSender);
        }

        @Test
        @DisplayName("evicts runtime configuration after authentication failure")
        void evictsRuntimeConfigurationAfterAuthenticationFailure() {
            // arrange
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(completeRuntimeConfig());
            doThrow(EmailTransportException.authenticationFailed(
                    new RuntimeException("bad credentials")))
                    .when(transportSender).send(ArgumentMatchers.any());
            // act + assert
            assertThatEmailExceptionThrownBy(() -> service.sendTestEmail(RECIPIENT))
                    .hasErrorCode(EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED);
            // verify
            verify(runtimeConfigResolver).evict();
        }

        @Test
        @DisplayName("translates generic transport failure")
        void translatesGenericTransportFailure() {
            // arrange
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(completeRuntimeConfig());
            doThrow(EmailTransportException.sendFailed(
                    new RuntimeException("send failed")))
                    .when(transportSender).send(ArgumentMatchers.any());
            // act + assert
            assertThatEmailExceptionThrownBy(() -> service.sendTestEmail(RECIPIENT))
                    .hasErrorCode(EmailErrorCode.EMAIL_SMTP_SEND_FAILED);
        }

    }

}
