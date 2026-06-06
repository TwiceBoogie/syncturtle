package com.syncturtle.services.email.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.dto.EmailEnvelope;
import com.syncturtle.services.email.exception.EmailTemplateException;
import com.syncturtle.services.email.service.EmailTemplateService;
import com.syncturtle.services.email.service.EmailTemplateService.RenderedEmail;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

    @Mock
    TemplateEngine templateEngine;

    @InjectMocks
    EmailTemplateService service;

    @Test
    void render_whenMagicLink_rendersHtmlAndTextTemplates() {
        // arrange
        EmailEnvelope envelope = EmailEnvelope.builder()
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject(" Your magic link ")
                .to(List.of("lunasnow@marvel.com"))
                .model(Map.of(
                        "firstName", "Luna",
                        "magicLink", "https://app.syncturtle.com/magic?token=abc"))
                .correlationId("corr-123")
                .build();
        when(templateEngine.process(eq("email/html/magic-link"), any(Context.class)))
                .thenReturn("<html><body>Hello Luna</body></html>");
        when(templateEngine.process(eq("email/text/magic-link"), any(Context.class)))
                .thenReturn("Hello Luna");
        // act
        RenderedEmail rendered = service.render(envelope);
        // assert
        assertThat(rendered.subject()).isEqualTo("Your magic link");
        assertThat(rendered.htmlBody()).contains("Hello Luna");
        assertThat(rendered.textBody()).contains("Hello Luna");

        ArgumentCaptor<Context> htmlCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<Context> textCaptor = ArgumentCaptor.forClass(Context.class);

        verify(templateEngine).process(eq("email/html/magic-link"), htmlCaptor.capture());
        verify(templateEngine).process(eq("email/text/magic-link"), textCaptor.capture());

        assertThat(htmlCaptor.getValue().getVariable("firstName")).isEqualTo("Luna");
        assertThat(htmlCaptor.getValue().getVariable("magicLink"))
                .isEqualTo("https://app.syncturtle.com/magic?token=abc");

        assertThat(textCaptor.getValue().getVariable("firstName")).isEqualTo("Luna");
        assertThat(textCaptor.getValue().getVariable("magicLink"))
                .isEqualTo("https://app.syncturtle.com/magic?token=abc");
    }

    @Test
    void render_whenPasswordResetRequested_throwsEmailTemplateException() {
        // arrange
        EmailEnvelope envelope = envelope(EmailTemplateType.PASSWORD_RESET);
        // act
        EmailTemplateException exception = catchThrowableOfType(
                EmailTemplateException.class,
                () -> service.render(envelope));
        // assert
        assertTemplateException(
                exception,
                EmailErrorCode.EMAIL_TEMPLATE_UNSUPPORTED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Email template is not supported.");
        assertThat(exception.getPayload()).containsEntry("template_type", "PASSWORD_RESET");
        // verify
        verifyNoInteractions(templateEngine);
    }

    @Test
    void render_whenVerifyEmailRequested_throwsEmailTemplateException() {
        // arrange
        EmailEnvelope envelope = envelope(EmailTemplateType.VERIFY_EMAIL);
        // act
        EmailTemplateException exception = catchThrowableOfType(EmailTemplateException.class,
                () -> service.render(envelope));
        // assert
        assertTemplateException(
                exception,
                EmailErrorCode.EMAIL_TEMPLATE_UNSUPPORTED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Email template is not supported.");
        assertThat(exception.getPayload()).containsEntry("template_type", "VERIFY_EMAIL");
        // verify
        verifyNoInteractions(templateEngine);
    }

    @Test
    void render_whenEnvelopeIsNull_throwsInvalidEnvelopeException() {
        // act
        EmailTemplateException exception = catchThrowableOfType(
                EmailTemplateException.class,
                () -> service.render(null));
        // assert
        assertTemplateException(
                exception,
                EmailErrorCode.EMAIL_ENVELOPE_INVALID,
                HttpStatus.BAD_REQUEST,
                "Email envelope is invalid.");
        assertThat(exception.getPayload()).containsEntry("reason", "envelope must not be null");
        // verify
        verifyNoInteractions(templateEngine);
    }

    @Test
    void render_whenSubjectBlank_throwsInvalidEnvelopeException() {
        // arrange
        EmailEnvelope envelope = EmailEnvelope.builder()
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject(" ")
                .to(List.of("lunasnow@marvel.com"))
                .model(Map.of())
                .correlationId("corr-123")
                .build();
        // act
        EmailTemplateException exception = catchThrowableOfType(
                EmailTemplateException.class,
                () -> service.render(envelope));
        assertThat(exception.getPayload()).containsEntry("reason", "subject must not be blank");
        // verify
        verifyNoInteractions(templateEngine);
    }

    @Test
    void render_whenRecipientListEmpty_throwsInvalidEnvelopeException() {
        // arrange
        EmailEnvelope envelope = EmailEnvelope.builder()
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject(" ")
                .to(List.of("lunasnow@marvel.com"))
                .model(Map.of())
                .correlationId("corr-123")
                .build();
        // act
        EmailTemplateException exception = catchThrowableOfType(
                EmailTemplateException.class,
                () -> service.render(envelope));
        // assert
        assertTemplateException(
                exception,
                EmailErrorCode.EMAIL_ENVELOPE_INVALID,
                HttpStatus.BAD_REQUEST,
                "Email envelope is invalid.");
        assertThat(exception.getPayload()).containsEntry("reason", "subject must not be blank");
        // verify
        verifyNoInteractions(templateEngine);
    }

    @Test
    void render_whenTemplateEngineFails_throwsRenderFailedException() {
        // arrange
        EmailEnvelope envelope = envelope(EmailTemplateType.MAGIC_LINK);
        RuntimeException thrown = new RuntimeException("thymeleaf failed");

        when(templateEngine.process(eq("email/html/magic-link"), any(Context.class))).thenThrow(thrown);
        // act
        EmailTemplateException exception = catchThrowableOfType(
                EmailTemplateException.class,
                () -> service.render(envelope));
        assertThat(exception.getPayload()).containsEntry("template_type", "MAGIC_LINK");
        assertThat(exception.getCause()).isSameAs(thrown);
    }

    private static void assertTemplateException(
            EmailTemplateException exception,
            EmailErrorCode expectedCode,
            HttpStatus expectedStatus,
            String expectedPublicMessage) {
        assertThat(exception).isNotNull();
        assertThat(exception.getEmailErrorCode()).isEqualTo(expectedCode);
        assertThat(exception.getErrorKey()).isEqualTo(expectedCode.getKey());
        assertThat(exception.getPublicMessage()).isEqualTo(expectedPublicMessage);
    }

    private static EmailEnvelope envelope(EmailTemplateType templateType) {
        return EmailEnvelope.builder()
                .templateType(templateType)
                .subject("Your magic link")
                .to(List.of("lunasnow@marvel.com"))
                .model(Map.of(
                        "firstName", "Luna",
                        "magicLink", "https://app.syncturtle.com/magic?token=abc"))
                .correlationId("corr-123")
                .build();
    }

}
