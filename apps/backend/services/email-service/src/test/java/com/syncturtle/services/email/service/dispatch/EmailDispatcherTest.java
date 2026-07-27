package com.syncturtle.services.email.service.dispatch;

import static com.syncturtle.services.email.support.assertion.EmailExceptionAssert.assertThatEmailExceptionThrownBy;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.RECIPIENTS;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeDispatchParam;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.renderedMagicCodeEmail;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.completeRuntimeConfig;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.disabledRuntimeConfig;
import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.incompleteRuntimeConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.exception.EmailTemplateException;
import com.syncturtle.services.email.exception.EmailTransportException;
import com.syncturtle.services.email.service.param.EmailDispatchParam;
import com.syncturtle.services.email.service.param.EmailTransportSendParam;
import com.syncturtle.services.email.service.result.RenderedEmailResult;
import com.syncturtle.services.email.service.runtime.EmailRuntimeConfigResolver;
import com.syncturtle.services.email.service.runtime.EmailRuntimeConfigSnapshot;
import com.syncturtle.services.email.service.template.EmailTemplateRenderer;
import com.syncturtle.services.email.service.transport.EmailTransportSender;
import com.syncturtle.services.email.support.fixture.EmailFixtures.MagicCode;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailDispatcher")
class EmailDispatcherTest {

    @Mock
    EmailRuntimeConfigResolver runtimeConfigResolver;
    @Mock
    EmailTemplateRenderer templateRenderer;
    @Mock
    EmailTransportSender transportSender;

    @Captor
    ArgumentCaptor<EmailTransportSendParam> transportParamCaptor;

    private EmailDispatcher dispatcher;

    @BeforeEach
    void setup() {
        dispatcher = new EmailDispatcher(runtimeConfigResolver, templateRenderer, transportSender);
    }

    @Nested
    @DisplayName("dispatch(EmailDispatchParam)")
    class DispatchTests {

        @Test
        @DisplayName("renders and sends email with resolved runtime configuration")
        void rendersAndSendsEmailWithResolvedRuntimeConfiguration() {
            // arrange
            EmailRuntimeConfigSnapshot configSnapshot = completeRuntimeConfig();
            EmailDispatchParam param = magicCodeDispatchParam();
            RenderedEmailResult renderedEmailResult = renderedMagicCodeEmail();
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(configSnapshot);
            when(templateRenderer.render(param)).thenReturn(renderedEmailResult);
            // act
            dispatcher.dispatch(param);
            // assert
            // verify + capture
            verify(transportSender).send(transportParamCaptor.capture());
            EmailTransportSendParam sent = transportParamCaptor.getValue();
            assertThat(sent.getRecipients()).isEqualTo(RECIPIENTS);
            assertThat(sent.getSubject()).isEqualTo(MagicCode.SUBJECT);
            assertThat(sent.getTextBody()).isEqualTo(MagicCode.TEXT_BODY);
            assertThat(sent.getHtmlBody()).isEqualTo(MagicCode.HTML_BODY);
        }

        @Test
        @DisplayName("throws SMTP disabled before rendering")
        void throwsSmtpDisabledBeforeRendering() {
            // arrange
            EmailRuntimeConfigSnapshot configSnapshot = disabledRuntimeConfig();
            EmailDispatchParam param = magicCodeDispatchParam();
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(configSnapshot);
            // act + assert
            assertThatEmailExceptionThrownBy(() -> dispatcher.dispatch(param))
                    .hasErrorCode(EmailErrorCode.EMAIL_SMTP_DISABLED);
            // verify
            verifyNoInteractions(templateRenderer, transportSender);
        }

        @Test
        @DisplayName("throws SMTP not configured before rendering")
        void throwsSmtpNotConfiguredBeforeRendering() {
            // arrange
            EmailRuntimeConfigSnapshot configSnapshot = incompleteRuntimeConfig();
            EmailDispatchParam param = magicCodeDispatchParam();
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(configSnapshot);
            // act + assert
            assertThatEmailExceptionThrownBy(() -> dispatcher.dispatch(param))
                    .hasErrorCode(EmailErrorCode.EMAIL_SMTP_NOT_CONFIGURED);
            // verify
            verifyNoInteractions(templateRenderer, transportSender);
        }

        @Test
        @DisplayName("translates template failure")
        void translatesTemplateFailure() {
            // arrange
            EmailRuntimeConfigSnapshot configSnapshot = completeRuntimeConfig();
            EmailDispatchParam param = magicCodeDispatchParam();
            RuntimeException cause = new RuntimeException("render failure");
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(configSnapshot);
            when(templateRenderer.render(param))
                    .thenThrow(EmailTemplateException.renderFailed(EmailTemplateType.MAGIC_CODE, cause));
            // act + assert
            assertThatEmailExceptionThrownBy(() -> dispatcher.dispatch(param))
                    .hasErrorCode(EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED);
            // verify
            verifyNoInteractions(transportSender);
        }

        @Test
        @DisplayName("evicts runtime cache and translates authentication failure")
        void evictsRuntimeCacheAndTranslatesAuthenticationFailure() {
            // arrange
            EmailRuntimeConfigSnapshot configSnapshot = completeRuntimeConfig();
            EmailDispatchParam param = magicCodeDispatchParam();
            RenderedEmailResult renderedEmailResult = renderedMagicCodeEmail();
            EmailTransportException transportException = EmailTransportException
                    .authenticationFailed(new RuntimeException("bad credentials"));
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(configSnapshot);
            when(templateRenderer.render(param)).thenReturn(renderedEmailResult);
            doThrow(transportException).when(transportSender).send(any());
            // act + assert
            assertThatEmailExceptionThrownBy(() -> dispatcher.dispatch(param))
                    .hasErrorCode(EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED);
            // verify
            verify(runtimeConfigResolver).evict();
        }

        @Test
        @DisplayName("translates transport send failure without eviction")
        void translatesTransportSendFailureWithoutEviction() {
            // arrange
            EmailRuntimeConfigSnapshot configSnapshot = completeRuntimeConfig();
            EmailDispatchParam param = magicCodeDispatchParam();
            RenderedEmailResult renderedEmailResult = renderedMagicCodeEmail();
            EmailTransportException transportException = EmailTransportException
                    .sendFailed(new RuntimeException("send failed"));
            // conditions
            when(runtimeConfigResolver.resolveCurrent()).thenReturn(configSnapshot);
            when(templateRenderer.render(param)).thenReturn(renderedEmailResult);
            doThrow(transportException).when(transportSender).send(any());
            // act + assert
            assertThatEmailExceptionThrownBy(() -> dispatcher.dispatch(param))
                    .hasErrorCode(EmailErrorCode.EMAIL_DISPATCH_FAILED);
            // verify
        }

    }

}
