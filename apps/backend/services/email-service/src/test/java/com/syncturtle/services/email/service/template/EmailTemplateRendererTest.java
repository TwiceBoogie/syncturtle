package com.syncturtle.services.email.service.template;

import static com.syncturtle.services.email.support.assertion.EmailExceptionAssert.assertThatEmailExceptionThrownBy;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.HTML_BODY;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.SUBJECT;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.TEXT_BODY;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.dispatchParam;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import com.syncturtle.common.contracts.email.error.EmailErrorCode;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.service.param.EmailDispatchParam;
import com.syncturtle.services.email.service.result.RenderedEmailResult;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailTemplateRenderer")
class EmailTemplateRendererTest {

    @Mock
    TemplateEngine templateEngine;

    private EmailTemplateRenderer renderer;

    @BeforeEach
    void setup() {
        renderer = new EmailTemplateRenderer(templateEngine);
    }

    @Nested
    @DisplayName("render(EmailDispatchParam)")
    class RenderTests {

        @Test
        @DisplayName("renders HTML and text templates")
        void rendersHtmlAndTextTemplates() {
            // arrange
            EmailDispatchParam param = dispatchParam();
            // conditions
            when(templateEngine.process(eq("email/html/magic-link"), any(IContext.class))).thenReturn(HTML_BODY);
            when(templateEngine.process(eq("email/text/magic-link"), any(IContext.class))).thenReturn(TEXT_BODY);
            // act
            RenderedEmailResult result = renderer.render(param);
            // assert
            assertThat(result.getSubject()).isEqualTo(SUBJECT);
            assertThat(result.getHtmlBody()).isEqualTo(HTML_BODY);
            assertThat(result.getTextBody()).isEqualTo(TEXT_BODY);
            // order + verify
            InOrder order = inOrder(templateEngine);
            order.verify(templateEngine).process(eq("email/html/magic-link"), any(IContext.class));
            order.verify(templateEngine).process(eq("email/text/magic-link"), any(IContext.class));
            verifyNoMoreInteractions(templateEngine);
        }

        @Test
        @DisplayName("throws unsupported template error before rendering")
        void throwsUnsupportedTemplateErrorBeforeRendering() {
            // arrange
            EmailDispatchParam param = EmailDispatchParam.builder()
                    .eventId("event-id")
                    .templateType(EmailTemplateType.VERIFY_EMAIL)
                    .subject("subject")
                    .recipients(List.of("lunasnow@marvel.com"))
                    .model(Map.of())
                    .build();
            // conditions
            // act + assert
            assertThatEmailExceptionThrownBy(() -> renderer.render(param))
                    .hasErrorCode(EmailErrorCode.EMAIL_TEMPLATE_UNSUPPORTED);
            // verify
            verifyNoMoreInteractions(templateEngine);
        }

        @Test
        @DisplayName("wraps template engine failure")
        void wrapsTemplateEngineFailure() {
            // arrange
            RuntimeException cause = new RuntimeException("template parse error");
            // conditions
            when(templateEngine.process(eq("email/html/magic-link"), any(IContext.class))).thenThrow(cause);
            // act + assert
            assertThatEmailExceptionThrownBy(() -> renderer.render(dispatchParam()))
                    .hasErrorCode(EmailErrorCode.EMAIL_TEMPLATE_RENDER_FAILED)
                    .hasCauseSameAs(cause);
        }

        @Test
        @DisplayName("rejects null dispatch parameter")
        void rejectsNullDispatchParameter() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> renderer.render(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("email dispatch param is required");
            // verify
        }

    }

}
