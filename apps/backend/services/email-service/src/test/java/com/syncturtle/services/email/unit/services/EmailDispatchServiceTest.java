package com.syncturtle.services.email.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.dto.EmailEnvelope;
import com.syncturtle.services.email.dto.EmailRuntimeConfig;
import com.syncturtle.services.email.exceptions.EmailDispatchException;
import com.syncturtle.services.email.exceptions.EmailTemplateException;
import com.syncturtle.services.email.service.DynamicMailSenderFactory;
import com.syncturtle.services.email.service.EmailDispatchService;
import com.syncturtle.services.email.service.EmailRuntimeConfigService;
import com.syncturtle.services.email.service.EmailTemplateService;
import com.syncturtle.services.email.service.EmailTemplateService.RenderedEmail;

import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailDispatchServiceTest {

    @Mock
    EmailRuntimeConfigService emailRuntimeConfigService;

    @Mock
    DynamicMailSenderFactory dynamicMailSenderFactory;

    @Mock
    EmailTemplateService emailTemplateService;

    @Mock
    JavaMailSender sender;

    @InjectMocks
    EmailDispatchService service;

    @Test
    void send_whenSmtpDisabled_throwsRetryableBeforeDoingAnythingElse() {
        // arrange
        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(disabledConfig());
        // act
        EmailDispatchException exception = catchThrowableOfType(
                EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertDispatchException(
                exception,
                EmailErrorCode.EMAIL_SMTP_DISABLED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "SMTP is disabled.",
                true,
                null);
        // verify
        verify(emailRuntimeConfigService).getCurrentConfig();
        verifyNoInteractions(emailTemplateService, dynamicMailSenderFactory, sender);
    }

    @Test
    void send_whenConfigIncomplete_throwsRetryable() {
        // arrange
        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(incompleteConfig());
        // act
        EmailDispatchException exception = catchThrowableOfType(EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertDispatchException(
                exception,
                EmailErrorCode.EMAIL_SMTP_NOT_CONFIGURED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "SMTP config is incomplete.",
                true,
                null);
        // verify
        verify(emailRuntimeConfigService).getCurrentConfig();
        verifyNoInteractions(emailTemplateService, dynamicMailSenderFactory, sender);
    }

    @Test
    void send_whenTemplateRenderingFails_throwsPermanentDispatchException() {
        // arrange
        EmailTemplateException thrown = EmailTemplateException
                .unsupportedTemplate(EmailTemplateType.PASSWORD_RESET);

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenThrow(thrown);
        // act
        EmailDispatchException exception = catchThrowableOfType(
                EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertDispatchException(
                exception,
                EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED,
                HttpStatus.BAD_REQUEST,
                "Email template rendering failed.",
                false,
                thrown);
        // verify
        verify(emailTemplateService).render(any());
        verify(dynamicMailSenderFactory, never()).create(any());
        verifyNoInteractions(sender);
    }

    @Test
    void send_whenAuthenticationFails_evictsCacheAndThrowsRetryable() {
        // arrange
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        MailAuthenticationException thrown = new MailAuthenticationException("bad credentials");

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(thrown).when(sender).send(mimeMessage);
        // act
        EmailDispatchException exception = catchThrowableOfType(
                EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertDispatchException(
                exception,
                EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "SMTP authentication failed.",
                true,
                thrown);
        // verify
        verify(emailRuntimeConfigService).evict();
    }

    @Test
    void send_whenMailSendHasAuthenticationRootCause_evictsCacheAndThrowsRetryable() {
        // arrange
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        MailSendException thrown = new MailSendException(
                "send failed",
                new MessagingException("wrapped",
                        new AuthenticationFailedException("bad credentials")));

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(thrown).when(sender).send(mimeMessage);
        // act
        EmailDispatchException exception = catchThrowableOfType(
                EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertDispatchException(
                exception,
                EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "SMTP authentication failed.",
                true,
                thrown);
        // verify
        verify(emailRuntimeConfigService).evict();
    }

    @Test
    void send_whenMailSendTimeoutOccurs_throwsRetryableTimeout() {
        // arrange
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        MailSendException thrown = new MailSendException(
                "send failed",
                new MessagingException("wrapped", new SocketTimeoutException("timed out")));

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(thrown).when(sender).send(mimeMessage);
        // act
        EmailDispatchException exception = catchThrowableOfType(
                EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertDispatchException(
                exception,
                EmailErrorCode.EMAIL_SMTP_TIMEOUT,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Timed out while sending email.",
                true,
                thrown);
    }

    @Test
    void send_whenMailSendConnectionFails_throwsRetryableConnectionFailure() {
        // arrange
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        MailSendException thrown = new MailSendException(
                "send failed",
                new MessagingException("wrapped", new ConnectException("connection refused")));

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(thrown).when(sender).send(mimeMessage);
        // act
        EmailDispatchException exception = catchThrowableOfType(
                EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertDispatchException(
                exception,
                EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Could not connect to the SMTP server.",
                true,
                thrown);
    }

    @Test
    void send_whenRecipientsRefused_throwsPermanentFailure() {
        // arrange
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        MailSendException thrown = new MailSendException(
                "send failed",
                new MessagingException("wrapped", new SendFailedException("all recipients refused")));

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(thrown).when(sender).send(mimeMessage);
        // act
        EmailDispatchException exception = catchThrowableOfType(
                EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertDispatchException(
                exception,
                EmailErrorCode.EMAIL_SMTP_RECIPIENTS_REFUSED,
                HttpStatus.BAD_REQUEST,
                "All recipient addresses were refused.",
                false,
                thrown);
    }

    @Test
    void send_whenMailSendFailsForUnknownReason_throwsRetryableGenericDispatchFailure() {
        // arrange
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        MailSendException thrown = new MailSendException("smtp unavailable");

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(thrown).when(sender).send(mimeMessage);
        // act
        EmailDispatchException exception = catchThrowableOfType(
                EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertDispatchException(
                exception,
                EmailErrorCode.EMAIL_DISPATCH_FAILED,
                HttpStatus.SERVICE_UNAVAILABLE,
                "Failed to send email.",
                true,
                thrown);
    }

    @Test
    void send_whenMimeMessageBuildFails_throwsPermanentMessageBuildFailure() throws Exception {
        // arrange
        MimeMessage mimeMessage = spy(new MimeMessage(Session.getInstance(new Properties())));
        MessagingException thrown = new MessagingException("subject failed");

        doThrow(thrown).when(mimeMessage).setSubject(anyString(), anyString());

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        // act
        EmailDispatchException exception = catchThrowableOfType(
                EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertDispatchException(
                exception,
                EmailErrorCode.EMAIL_MESSAGE_BUILD_FAILED,
                HttpStatus.BAD_REQUEST,
                "Failed to build email message.",
                false,
                thrown);
        // verify
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test
    void send_whenSuccessful_buildsMimeMessageAndSendsIt() throws Exception {
        // arrange
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        // act
        service.send(envelope());
        // assert
        verify(sender).send(mimeMessage);
        assertThat(mimeMessage.getSubject()).isEqualTo("Your magic link");
        assertThat(Arrays.stream(mimeMessage.getFrom()))
                .map(Address::toString)
                .containsExactly("no-reply@syncturtle.com");
        assertThat(Arrays.stream(mimeMessage.getAllRecipients())
                .map(Address::toString))
                .containsExactly("lunasnow@marvel.com");
    }

    private static void assertDispatchException(
            EmailDispatchException exception,
            EmailErrorCode expectedCode,
            HttpStatus expectedStatus,
            String expectedPublicMessage,
            boolean expectedRetryable,
            Throwable expectedCause) {
        assertThat(exception).isNotNull();
        assertThat(exception.getEmailErrorCode()).isEqualTo(expectedCode);
        assertThat(exception.getErrorCode()).isEqualTo(expectedCode);
        assertThat(exception.getCode()).isEqualTo(expectedCode.getCode());
        assertThat(exception.getMessage()).isEqualTo(expectedCode.getKey());
        assertThat(exception.getPublicMessage()).isEqualTo(expectedPublicMessage);
        assertThat(exception.getStatus()).isEqualTo(expectedStatus);
        assertThat(exception.isRetryable()).isEqualTo(expectedRetryable);

        if (expectedCause == null) {
            assertThat(exception.getCause()).isNull();
        } else {
            assertThat(exception.getCause()).isSameAs(expectedCause);
        }
    }

    private static EmailEnvelope envelope() {
        return EmailEnvelope.builder()
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject("Your magic link")
                .to(List.of("lunasnow@marvel.com"))
                .model(Map.of(
                        "firstName", "Luna",
                        "magicLink", "https://app.syncturtle.com/magic?token=abc"))
                .correlationId("corr-123")
                .build();
    }

    private static RenderedEmail renderedEmail() {
        return new RenderedEmail(
                "Your magic link",
                "<html><body>Hello Luna</body></html",
                "Hello Luna");
    }

    private static EmailRuntimeConfig completeEnabledConfig() {
        return EmailRuntimeConfig.builder()
                .enabled(true)
                .host("smtp.example.com")
                .port(587)
                .username("mailer")
                .password("secret")
                .from("no-reply@syncturtle.com")
                .useTls(true)
                .useSsl(false)
                .version(9L)
                .build();
    }

    private static EmailRuntimeConfig disabledConfig() {
        return EmailRuntimeConfig.builder()
                .enabled(false)
                .host("smtp.example.com")
                .port(587)
                .username("mailer")
                .password("secret")
                .from("no-reply@syncturtle.com")
                .useTls(true)
                .useSsl(false)
                .version(9L)
                .build();
    }

    private static EmailRuntimeConfig incompleteConfig() {
        return EmailRuntimeConfig.builder()
                .enabled(true)
                .host("")
                .port(587)
                .username("mailer")
                .password("secret")
                .from("no-reply@syncturtle.com")
                .useTls(true)
                .useSsl(false)
                .version(9L)
                .build();
    }

}
