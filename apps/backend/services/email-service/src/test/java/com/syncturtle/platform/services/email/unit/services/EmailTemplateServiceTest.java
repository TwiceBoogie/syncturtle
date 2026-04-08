package com.syncturtle.platform.services.email.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.syncturtle.common.core.enums.EmailTemplateType;
import com.syncturtle.platform.services.email.dto.EmailEnvelope;
import com.syncturtle.platform.services.email.service.EmailTemplateService;
import com.syncturtle.platform.services.email.service.EmailTemplateService.RenderedEmail;

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
                .subject("Your magic link")
                .to(List.of("user@example.com"))
                .model(Map.of(
                        "firstName", "Luna",
                        "magicLink", "https://app.syncturtle.com/magic?token=abc"))
                .correlationId("corr-123")
                .build();
        when(templateEngine.process(eq("email/magic-link"), any(Context.class)))
                .thenReturn("<html><body>Hello Luna</body></html>");
        when(templateEngine.process(eq("email/magic-link.txt"), any(Context.class))).thenReturn("Hello Luna");
        // act
        RenderedEmail rendered = service.render(envelope);
        // assert
        assertThat(rendered.subject()).isEqualTo("Your magic link");
        assertThat(rendered.htmlBody()).contains("Hello Luna");
        assertThat(rendered.textBody()).contains("Hello Luna");

        ArgumentCaptor<Context> htmlCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<Context> textCaptor = ArgumentCaptor.forClass(Context.class);

        verify(templateEngine).process(eq("email/magic-link"), htmlCaptor.capture());
        verify(templateEngine).process(eq("email/magic-link.txt"), textCaptor.capture());

        assertThat(htmlCaptor.getValue().getVariable("firstName")).isEqualTo("Luna");
        assertThat(htmlCaptor.getValue().getVariable("magicLink"))
                .isEqualTo("https://app.syncturtle.com/magic?token=abc");

        assertThat(textCaptor.getValue().getVariable("firstName")).isEqualTo("Luna");
        assertThat(textCaptor.getValue().getVariable("magicLink"))
                .isEqualTo("https://app.syncturtle.com/magic?token=abc");
    }

    @Test
    void render_whenPasswordResetRequested_throwsUnsupportedOperationException() {
        // arrange
        EmailEnvelope envelope = EmailEnvelope.builder()
                .templateType(EmailTemplateType.PASSWORD_RESET)
                .subject("Reset password")
                .to(List.of("user@example.com"))
                .model(Map.of())
                .correlationId("corr-123")
                .build();
        // act + assert
        assertThatThrownBy(() -> service.render(envelope))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("PASSWORD_RESET not implemented yet");
    }

    @Test
    void render_whenVerifyEmailRequested_throwsUnsupportedOperationException() {
        // arrange
        EmailEnvelope envelope = EmailEnvelope.builder()
                .templateType(EmailTemplateType.VERIFY_EMAIL)
                .subject("Verify email")
                .to(List.of("user@example.com"))
                .model(Map.of())
                .correlationId("corr-123")
                .build();
        // act + assert
        assertThatThrownBy(() -> service.render(envelope))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("VERIFY_EMAIL not implemented yet");
    }

}
