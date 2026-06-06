package com.syncturtle.services.email.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.services.email.dto.EmailRuntimeConfig;
import com.syncturtle.services.email.exceptions.EmailCredentialCheckException;
import com.syncturtle.services.email.service.DynamicMailSenderFactory;
import com.syncturtle.services.email.service.EmailCredentialCheckService;
import com.syncturtle.services.email.service.EmailRuntimeConfigService;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;

@ExtendWith(MockitoExtension.class)
class EmailCredentialCheckServiceTest {

    @Mock
    EmailRuntimeConfigService emailRuntimeConfigService;

    @Mock
    DynamicMailSenderFactory dynamicMailSenderFactory;

    @InjectMocks
    EmailCredentialCheckService service;

    @Test
    void sendTestEmail_whenSmtpDisabled_throwsEmailCredentialCheckException() {
        // arrange
        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(disabledConfig());
        // act
        EmailCredentialCheckException exception = catchThrowableOfType(
                EmailCredentialCheckException.class,
                () -> service.sendTestEmail("lunasnow@marvel.com"));
        // assert
        assertEmailCredentialException(
                exception,
                EmailErrorCode.EMAIL_SMTP_DISABLED,
                HttpStatus.BAD_REQUEST,
                "SMTP is disabled.",
                null);
        // verify
        verifyNoInteractions(dynamicMailSenderFactory);
    }

    @Test
    void sendTestEmail_whenConfigIsIncomplete_throwsEmailCredentialCheckException() {
        // arrange
        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(incompleteConfig());
        // act
        EmailCredentialCheckException exception = catchThrowableOfType(
                EmailCredentialCheckException.class,
                () -> service.sendTestEmail("lunasnow@marvel.com"));
        // assert
        assertEmailCredentialException(
                exception,
                EmailErrorCode.EMAIL_SMTP_NOT_CONFIGURED,
                HttpStatus.BAD_REQUEST,
                "Could not send email. Please check your configuration.",
                null);
        // verify
        verifyNoInteractions(dynamicMailSenderFactory);
    }

    @Test
    void sendTestEmail_whenMailAuthenticationExceptionOccurs_evictsConfigCacheAndThrows() {
        // arrange
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        MailAuthenticationException thrown = new MailAuthenticationException("bad credentials");

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(thrown).when(sender).send(mimeMessage);
        // act
        EmailCredentialCheckException exception = catchThrowableOfType(
                EmailCredentialCheckException.class,
                () -> service.sendTestEmail("lunasnow@marvel.com"));
        // assert
        assertEmailCredentialException(
                exception,
                EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED,
                HttpStatus.BAD_REQUEST,
                "SMTP authentication failed.",
                thrown);
        // verify
        verify(emailRuntimeConfigService).evict();
    }

    @ParameterizedTest(name = "{index} => root={0}, expectedCode={1}, shouldEvict={4}")
    @MethodSource("mailSendExceptionCases")
    @DisplayName("sendTestEmail translates MailSendException based on deepestroot cause")
    void sendTestEmail_whenMailSendExceptionOccurs_translatesByRootCause(
            Throwable rootCause,
            EmailErrorCode expectedCode,
            HttpStatus expectedStatus,
            String expectedPublicMessage,
            boolean shouldEvictCache) {
        // arrange
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        MailSendException thrown = new MailSendException("send failed", wrap(rootCause));

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(thrown).when(sender).send(mimeMessage);
        // act
        EmailCredentialCheckException exception = catchThrowableOfType(
                EmailCredentialCheckException.class,
                () -> service.sendTestEmail("user@example.com"));
        // assert
        assertEmailCredentialException(
                exception,
                expectedCode,
                expectedStatus,
                expectedPublicMessage,
                thrown);

        // verify
        if (shouldEvictCache) {
            verify(emailRuntimeConfigService).evict();
        } else {
            verify(emailRuntimeConfigService, never()).evict();
        }
    }

    @ParameterizedTest(name = "{index} => root={0}, expectedCode={1}")
    @MethodSource("messagingExceptionCases")
    @DisplayName("sendTestEmail translates MessagingException based on deepestroot cause")
    void sendTestEmail_whenMessagingExceptionOccurs_translatesByRootCause(
            Exception rootCause,
            EmailErrorCode expectedCode,
            HttpStatus expectedStatus,
            String expectedPublicMessage)
            throws Exception {
        // arrange
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = spy(new MimeMessage(Session.getInstance(new Properties())));
        MessagingException thrown = new MessagingException("subject failed", wrap(rootCause));

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(thrown).when(mimeMessage).setSubject(anyString(), anyString());
        // act
        EmailCredentialCheckException exception = catchThrowableOfType(
                EmailCredentialCheckException.class,
                () -> service.sendTestEmail("lunasnow@marvel.com"));
        // assert
        assertEmailCredentialException(
                exception,
                expectedCode,
                expectedStatus,
                expectedPublicMessage,
                thrown);
        // verify
        verify(sender, never()).send(any(MimeMessage.class));
        verify(emailRuntimeConfigService, never()).evict();
    }

    @Test
    void sendTestEmail_whenUnexpectedRuntimeExceptionOccurs_returnsGenericFallback() {
        // arrange
        JavaMailSender sender = mock(JavaMailSender.class);
        RuntimeException thrown = new RuntimeException("error");

        when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
        when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
        when(sender.createMimeMessage()).thenThrow(thrown);
        // act
        EmailCredentialCheckException exception = catchThrowableOfType(
                EmailCredentialCheckException.class,
                () -> service.sendTestEmail("lunasnow@marvel.com"));
        // assert
        assertEmailCredentialException(
                exception,
                EmailErrorCode.EMAIL_SMTP_SEND_FAILED,
                HttpStatus.BAD_GATEWAY,
                "Could not send email. Please check your configuration.",
                thrown);
    }

    private static void assertEmailCredentialException(
            EmailCredentialCheckException exception,
            EmailErrorCode expectedCode,
            HttpStatus expectedStatus,
            String expectedPublicMessage,
            Throwable expectedCause) {
        assertThat(exception).isNotNull();
        assertThat(exception.getEmailErrorCode()).isEqualTo(expectedCode);
        assertThat(exception.getErrorCode()).isEqualTo(expectedCode);
        assertThat(exception.getCode()).isEqualTo(expectedCode.getCode());
        assertThat(exception.getMessage()).isEqualTo(expectedCode.getKey());
        assertThat(exception.getErrorMessage()).isEqualTo(expectedCode.getKey());
        assertThat(exception.getStatus()).isEqualTo(expectedStatus);
        assertThat(exception.getPublicMessage()).isEqualTo(expectedPublicMessage);

        if (expectedCause == null) {
            assertThat(exception.getCause()).isNull();
        } else {
            assertThat(exception.getCause()).isSameAs(expectedCause);
        }
    }

    private static Exception wrap(Throwable root) {
        if (root instanceof Exception ex) {
            return new Exception("wrapper", ex);
        }
        return new Exception("wrapper", new RuntimeException(root));
    }

    private static Stream<Arguments> mailSendExceptionCases() {
        return Stream.of(
                Arguments.of(
                        new AuthenticationFailedException("bad credentials"),
                        EmailErrorCode.EMAIL_SMTP_AUTHENTICATION_FAILED,
                        HttpStatus.BAD_REQUEST,
                        "SMTP authentication failed.",
                        true),
                Arguments.of(
                        new SocketTimeoutException("timed out"),
                        EmailErrorCode.EMAIL_SMTP_TIMEOUT,
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Timed out while checking SMTP credentials.",
                        false),
                Arguments.of(
                        new TimeoutException("timed out"),
                        EmailErrorCode.EMAIL_SMTP_TIMEOUT,
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Timed out while checking SMTP credentials.",
                        false),
                Arguments.of(
                        new UnknownHostException("smtp.example.com"),
                        EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED,
                        HttpStatus.BAD_GATEWAY,
                        "Could not connect to the SMTP server.",
                        false),
                Arguments.of(
                        new ConnectException("connection refused"),
                        EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED,
                        HttpStatus.BAD_GATEWAY,
                        "Could not connect to the SMTP server.",
                        false),
                Arguments.of(
                        new NoRouteToHostException("no route"),
                        EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED,
                        HttpStatus.BAD_GATEWAY,
                        "Could not connect to the SMTP server.",
                        false),
                Arguments.of(
                        new SendFailedException("all recipients refused"),
                        EmailErrorCode.EMAIL_SMTP_RECIPIENTS_REFUSED,
                        HttpStatus.BAD_REQUEST,
                        "All recipient addresses were refused.",
                        false),
                Arguments.of(
                        new IllegalStateException("unexpected mail send failure"),
                        EmailErrorCode.EMAIL_SMTP_SEND_FAILED,
                        HttpStatus.BAD_GATEWAY,
                        "Could not send email. Please check your configuration.",
                        false));
    }

    private static Stream<Arguments> messagingExceptionCases() {
        return Stream.of(
                Arguments.of(
                        new AddressException("invalid from"),
                        EmailErrorCode.EMAIL_SMTP_INVALID_FROM_ADDRESS,
                        HttpStatus.BAD_REQUEST,
                        "From address is invalid."),
                Arguments.of(
                        new SocketTimeoutException("timed out"),
                        EmailErrorCode.EMAIL_SMTP_TIMEOUT,
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Timed out while checking SMTP credentials."),
                Arguments.of(
                        new TimeoutException("timed out"),
                        EmailErrorCode.EMAIL_SMTP_TIMEOUT,
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Timed out while checking SMTP credentials."),
                Arguments.of(
                        new UnknownHostException("smtp.example.com"),
                        EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED,
                        HttpStatus.BAD_GATEWAY,
                        "Could not connect to the SMTP server."),
                Arguments.of(
                        new ConnectException("connection refused"),
                        EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED,
                        HttpStatus.BAD_GATEWAY,
                        "Could not connect to the SMTP server."),
                Arguments.of(
                        new NoRouteToHostException("no route"),
                        EmailErrorCode.EMAIL_SMTP_CONNECTION_FAILED,
                        HttpStatus.BAD_GATEWAY,
                        "Could not connect to the SMTP server."),
                Arguments.of(
                        new IllegalStateException("unexpected messaging failure"),
                        EmailErrorCode.EMAIL_SMTP_SEND_FAILED,
                        HttpStatus.BAD_GATEWAY,
                        "Could not send email. Please check your configuration."));
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

}
