package com.syncturtle.platform.services.email.unit.services;

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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import com.syncturtle.common.core.enums.EmailTemplateType;
import com.syncturtle.platform.services.email.dto.EmailEnvelope;
import com.syncturtle.platform.services.email.dto.EmailRuntimeConfig;
import com.syncturtle.platform.services.email.exceptions.EmailDispatchException;
import com.syncturtle.platform.services.email.service.DynamicMailSenderFactory;
import com.syncturtle.platform.services.email.service.EmailDispatchService;
import com.syncturtle.platform.services.email.service.EmailRuntimeConfigService;
import com.syncturtle.platform.services.email.service.EmailTemplateService;
import com.syncturtle.platform.services.email.service.EmailTemplateService.RenderedEmail;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
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
        EmailDispatchException exception = catchThrowableOfType(EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertThat(exception).isNotNull();
        assertThat(exception.isRetryable()).isTrue();
        assertThat(exception).hasMessageContaining("SMTP is disabled");
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
        assertThat(exception).isNotNull();
        assertThat(exception.isRetryable()).isTrue();
        assertThat(exception).hasMessageContaining("SMTP config is incomplete");
        // verify
        verify(emailRuntimeConfigService).getCurrentConfig();
        verifyNoInteractions(emailTemplateService, dynamicMailSenderFactory, sender);
    }

    @Test
    void send_whenTemplateRenderingFails_throwsPermanent() {
        // arrange
        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenThrow(new UnsupportedOperationException("not implemented"));
        // act
        EmailDispatchException exception = catchThrowableOfType(EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertThat(exception).isNotNull();
        assertThat(exception.isRetryable()).isFalse();
        assertThat(exception).hasMessageContaining("Email template rendering failed");
        assertThat(exception.getCause()).isInstanceOf(UnsupportedOperationException.class);
        // verify
        verify(emailTemplateService).render(any());
        verify(dynamicMailSenderFactory, never()).create(any());
        verifyNoInteractions(sender);
    }

    @Test
    void send_whenAuthenticationFails_evictsCacheAndThrowsRetryable() {
        // arrange
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailAuthenticationException("bad credentials")).when(sender).send(mimeMessage);
        // act
        EmailDispatchException exception = catchThrowableOfType(EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertThat(exception).isNotNull();
        assertThat(exception.isRetryable()).isTrue();
        assertThat(exception).hasMessageContaining("SMTP authentication failed");
        // verify
        verify(emailRuntimeConfigService).evict();
    }

    @Test
    void send_whenMailSendFails_throwsRetryable() {
        // arrange
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("smtp unavailable")).when(sender).send(mimeMessage);
        // act
        EmailDispatchException exception = catchThrowableOfType(EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertThat(exception).isNotNull();
        assertThat(exception.isRetryable()).isTrue();
        assertThat(exception).hasMessageContaining("Failed to send email");
    }

    @Test
    void send_whenMimeMessageBuildFails_throwsPermanent() throws Exception {
        // arrange
        MimeMessage mimeMessage = spy(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MessagingException("subject failed")).when(mimeMessage).setSubject(anyString(), anyString());
        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(emailTemplateService.render(any())).thenReturn(renderedEmail());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        // act
        EmailDispatchException exception = catchThrowableOfType(EmailDispatchException.class,
                () -> service.send(envelope()));
        // assert
        assertThat(exception).isNotNull();
        assertThat(exception.isRetryable()).isFalse();
        assertThat(exception).hasMessageContaining("Failed to build email message");
        assertThat(exception.getCause()).isInstanceOf(MessagingException.class);
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
                .containsExactly("user@example.com");
    }

    private static EmailEnvelope envelope() {
        return EmailEnvelope.builder()
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject("Your magic link")
                .to(List.of("user@example.com"))
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
