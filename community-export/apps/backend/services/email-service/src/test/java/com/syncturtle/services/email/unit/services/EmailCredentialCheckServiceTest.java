// package com.syncturtle.services.email.unit.services;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.assertj.core.api.Assertions.catchThrowableOfType;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.doThrow;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.never;
// import static org.mockito.Mockito.spy;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.verifyNoInteractions;
// import static org.mockito.Mockito.when;

// import java.net.ConnectException;
// import java.net.NoRouteToHostException;
// import java.net.SocketTimeoutException;
// import java.net.UnknownHostException;
// import java.util.Properties;
// import java.util.concurrent.TimeoutException;
// import java.util.stream.Stream;

// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.provider.Arguments;
// import org.junit.jupiter.params.provider.MethodSource;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.http.HttpStatus;
// import org.springframework.mail.MailAuthenticationException;
// import org.springframework.mail.MailSendException;
// import org.springframework.mail.javamail.JavaMailSender;

// import com.syncturtle.services.email.dto.EmailRuntimeConfig;
// import
// com.syncturtle.services.email.exceptions.EmailCredentialCheckException;
// import com.syncturtle.services.email.service.DynamicMailSenderFactory;
// import com.syncturtle.services.email.service.EmailCredentialCheckService;
// import com.syncturtle.services.email.service.EmailRuntimeConfigService;

// import jakarta.mail.AuthenticationFailedException;
// import jakarta.mail.MessagingException;
// import jakarta.mail.SendFailedException;
// import jakarta.mail.Session;
// import jakarta.mail.internet.AddressException;
// import jakarta.mail.internet.MimeMessage;

// @ExtendWith(MockitoExtension.class)
// class EmailCredentialCheckServiceTest {

// @Mock
// EmailRuntimeConfigService emailRuntimeConfigService;

// @Mock
// DynamicMailSenderFactory dynamicMailSenderFactory;

// @InjectMocks
// EmailCredentialCheckService service;

// @Test
// void sendTestEmail_whenSmtpDisabled_throwsEmailCredentialCheckException() {
// // arrange
// when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(disabledConfig());
// // act + assert
// assertThatThrownBy(() -> service.sendTestEmail("user@example.com"))
// .isInstanceOf(EmailCredentialCheckException.class)
// .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
// .hasMessage("SMTP is disabled.");
// // verify
// verifyNoInteractions(dynamicMailSenderFactory);
// }

// @Test
// void
// sendTestEmail_whenConfigIsIncomplete_throwsEmailCredentialCheckException() {
// // arrange
// when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(incompleteConfig());
// // act + assert
// assertThatThrownBy(() -> service.sendTestEmail("user@example.com"))
// .isInstanceOf(EmailCredentialCheckException.class)
// .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST)
// .hasMessage("Could not send email. Please check your configuration.");
// // verify
// verifyNoInteractions(dynamicMailSenderFactory);
// }

// @Test
// void
// sendTestEmail_whenMailAuthenticationExceptionOccurs_evictsConfigCacheAndThrows()
// {
// // arrange
// JavaMailSender sender = mock(JavaMailSender.class);
// MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new
// Properties()));

// when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
// when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
// when(sender.createMimeMessage()).thenReturn(mimeMessage);
// doThrow(new MailAuthenticationException("bad
// credentials")).when(sender).send(mimeMessage);
// // act
// EmailCredentialCheckException exception =
// catchThrowableOfType(EmailCredentialCheckException.class,
// () -> service.sendTestEmail("user@example.com"));
// // assert
// assertThat(exception).isNotNull();
// assertThat(exception).hasMessage("Invalid credentials provided");
// assertThat(exception.getCause()).isInstanceOf(MailAuthenticationException.class);
// // verify
// verify(emailRuntimeConfigService).evict();
// }

// @ParameterizedTest(name = "{index} => root={0}, expectedMessage={1},
// shouldEvict={2}")
// @MethodSource("mailSendExceptionCases")
// @DisplayName("sendTestEmail translates MailSendException based on deepest
// root cause")
// void sendTestEmail_whenMailSendExceptionOccurs_translatesByRootCause(
// Throwable rootCause,
// String expectedMessage,
// boolean shouldEvictCache) {
// // arrange
// JavaMailSender sender = mock(JavaMailSender.class);
// MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new
// Properties()));
// MailSendException mailSendException = new MailSendException("send failed",
// wrap(rootCause));

// when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
// when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
// when(sender.createMimeMessage()).thenReturn(mimeMessage);
// doThrow(mailSendException).when(sender).send(mimeMessage);
// // act
// EmailCredentialCheckException exception =
// catchThrowableOfType(EmailCredentialCheckException.class,
// () -> service.sendTestEmail("user@example.com"));
// // assert
// assertThat(exception).isNotNull();
// assertThat(exception).hasMessage(expectedMessage);
// assertThat(exception.getCause()).isSameAs(mailSendException);

// if (shouldEvictCache) {
// verify(emailRuntimeConfigService).evict();
// } else {
// verify(emailRuntimeConfigService, never()).evict();
// }
// }

// @ParameterizedTest(name = "{index} => root={0}, expectedMessage={1}")
// @MethodSource("messagingExceptionCases")
// @DisplayName("sendTestEmail translates MessagingException based on deepest
// root cause")
// void
// sendTestEmail_whenMessagingExceptionOccurs_translatesByRootCause(Exception
// rootCause,
// String expectedMessage)
// throws Exception {
// // arrange
// JavaMailSender sender = mock(JavaMailSender.class);
// MimeMessage mimeMessage = spy(new MimeMessage(Session.getInstance(new
// Properties())));

// when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
// when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
// when(sender.createMimeMessage()).thenReturn(mimeMessage);
// doThrow(new MessagingException("subject failed", wrap(rootCause)))
// .when(mimeMessage).setSubject(anyString(), anyString());
// // act
// EmailCredentialCheckException exception =
// catchThrowableOfType(EmailCredentialCheckException.class,
// () -> service.sendTestEmail("user@example.com"));
// // assert
// assertThat(exception).isNotNull();
// assertThat(exception).hasMessage(expectedMessage);
// assertThat(exception.getCause()).isInstanceOf(MessagingException.class);
// // verify
// verify(sender, never()).send(any(MimeMessage.class));
// verify(emailRuntimeConfigService, never()).evict();
// }

// @Test
// void
// sendTestEmail_whenUnexpectedRuntimeExceptionOccurs_returnsGenericFallback() {
// // arrange
// JavaMailSender sender = mock(JavaMailSender.class);
// when(emailRuntimeConfigService.getCurrentConfig()).thenReturn(completeEnabledConfig());
// when(dynamicMailSenderFactory.create(any())).thenReturn(sender);
// when(sender.createMimeMessage()).thenThrow(new RuntimeException("error"));
// // act
// EmailCredentialCheckException exception =
// catchThrowableOfType(EmailCredentialCheckException.class,
// () -> service.sendTestEmail("user@example.com"));
// // assert
// assertThat(exception).isNotNull();
// assertThat(exception).hasMessage("Could not send email. Please check your
// configuration.");
// assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
// assertThat(exception.getCause()).hasMessage("error");
// }

// private static Exception wrap(Throwable root) {
// if (root instanceof Exception ex) {
// return new Exception("wrapper", ex);
// }
// return new Exception("wrapper", new RuntimeException(root));
// }

// private static Stream<Arguments> mailSendExceptionCases() {
// return Stream.of(
// Arguments.of(
// new AuthenticationFailedException("bad credentials"),
// "Invalid credentials provided",
// true),
// Arguments.of(
// new SocketTimeoutException("timed out"),
// "Timeout error while trying to connect to the SMTP server",
// false),
// Arguments.of(
// new TimeoutException("timed out"),
// "Timeout error while trying to connect to the SMTP server",
// false),
// Arguments.of(
// new UnknownHostException("smtp.example.com"),
// "Could not connect with the SMTP server.",
// false),
// Arguments.of(
// new ConnectException("connection refused"),
// "Could not connect with the SMTP server.",
// false),
// Arguments.of(
// new NoRouteToHostException("no route"),
// "Could not connect with the SMTP server.",
// false),
// Arguments.of(
// new SendFailedException("all recipients refused"),
// "All recipient addresses were refused.",
// false),
// Arguments.of(
// new IllegalStateException("unexpected mail send failure"),
// "Could not send email. Please check your configuration.",
// false));
// }

// private static Stream<Arguments> messagingExceptionCases() {
// return Stream.of(
// Arguments.of(
// new AddressException("invalid from"),
// "From address is invalid."),
// Arguments.of(
// new SocketTimeoutException("timed out"),
// "Timeout error while trying to connect to the SMTP server."),
// Arguments.of(
// new TimeoutException("timed out"),
// "Timeout error while trying to connect to the SMTP server."),
// Arguments.of(
// new UnknownHostException("smtp.example.com"),
// "Could not connect with the SMTP server."),
// Arguments.of(
// new ConnectException("connection refused"),
// "Could not connect with the SMTP server."),
// Arguments.of(
// new NoRouteToHostException("no route"),
// "Could not connect with the SMTP server."),
// Arguments.of(
// new IllegalStateException("unexpected messaging failure"),
// "Could not send email. Please check your configuration."));
// }

// private static EmailRuntimeConfig disabledConfig() {
// return EmailRuntimeConfig.builder()
// .enabled(false)
// .host("smtp.example.com")
// .port(587)
// .username("mailer")
// .password("secret")
// .from("no-reply@syncturtle.com")
// .useTls(true)
// .useSsl(false)
// .version(9L)
// .build();
// }

// private static EmailRuntimeConfig incompleteConfig() {
// return EmailRuntimeConfig.builder()
// .enabled(true)
// .host("")
// .port(587)
// .username("mailer")
// .password("secret")
// .from("no-reply@syncturtle.com")
// .useTls(true)
// .useSsl(false)
// .version(9L)
// .build();
// }

// private static EmailRuntimeConfig completeEnabledConfig() {
// return EmailRuntimeConfig.builder()
// .enabled(true)
// .host("smtp.example.com")
// .port(587)
// .username("mailer")
// .password("secret")
// .from("no-reply@syncturtle.com")
// .useTls(true)
// .useSsl(false)
// .version(9L)
// .build();
// }

// }
