package com.syncturtle.services.email.service.template;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.RECIPIENT;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeDispatchParam;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeDispatchparam;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.syncturtle.services.email.configuration.email.EmailTemplateConfiguration;
import com.syncturtle.services.email.service.param.EmailDispatchParam;
import com.syncturtle.services.email.service.result.RenderedEmailResult;
import com.syncturtle.services.email.support.fixture.EmailFixtures.MagicCode;

@SpringJUnitConfig(classes = {
        EmailTemplateConfiguration.class,
        EmailTemplateRenderer.class
})
@DisplayName("EmailTemplateRenderer")
class EmailTemplateRendererIT {

    @Autowired
    EmailTemplateRenderer renderer;

    @Nested
    @DisplayName("render(EmailDispatchParam)")
    class RenderTests {

        @Test
        @DisplayName("resolves and renders magic-link HTML and text resources")
        void resolvesAndRendersMagicLinkHtmlAndTextResources() {
            // arrange
            EmailDispatchParam param = magicCodeDispatchParam();
            // conditions
            // act
            RenderedEmailResult result = renderer.render(param);
            // assert
            assertRendered(result, MagicCode.SUBJECT);
            assertThat(result.getHtmlBody())
                    .contains(MagicCode.CODE)
                    .contains(String.valueOf(MagicCode.EXPIRES_IN_MINUTES))
                    .contains(RECIPIENT);
            assertThat(result.getTextBody()).contains(MagicCode.TEXT_BODY);
            // verify
        }

        @Test
        @DisplayName("removes Thymeleaf processing attributes from rendered HTML")
        void removesThymeleafProcessingAttributesFromRenderedHtml() {
            // arrange
            EmailDispatchParam param = magicCodeDispatchParam();
            // conditions
            // act
            RenderedEmailResult result = renderer.render(param);
            // assert
            assertThat(result.getHtmlBody())
                    .doesNotContain("th:text=")
                    .doesNotContain("${code}")
                    .doesNotContain("${email}")
                    .doesNotContain("${expiresInMinutes}");
        }

        @Test
        @DisplayName("uses the model supplied for each rendering operation")
        void usesTheModelSuppliedForEachRenderingOperation() {
            // arrange
            EmailDispatchParam firstParam = magicCodeDispatchParam();
            String secondChoice = "abcd-efgh-ijkl";
            EmailDispatchParam secondParam = magicCodeDispatchparam(Map.of(
                    "email", RECIPIENT,
                    "code", secondChoice,
                    "expiresInMinutes", 15));
            // conditions
            // act
            RenderedEmailResult firstResult = renderer.render(firstParam);
            RenderedEmailResult secondResult = renderer.render(secondParam);
            // assert
            assertThat(firstResult.getHtmlBody())
                    .contains(MagicCode.CODE)
                    .doesNotContain(secondChoice);
            assertThat(secondResult.getHtmlBody())
                    .contains(secondChoice)
                    .doesNotContain(MagicCode.CODE);
            assertThat(secondResult.getTextBody())
                    .contains(secondChoice)
                    .contains("15");
        }

        @Test
        @DisplayName("does not modify the supplied model")
        void doesNotModifyTheSuppliedModel() {
            // arrange
            Map<String, Object> model = new LinkedHashMap<>();
            model.put("email", RECIPIENT);
            model.put("code", MagicCode.CODE);
            model.put("expiresInMinutes", MagicCode.EXPIRES_IN_MINUTES);

            Map<String, Object> originalModel = Map.copyOf(model);

            EmailDispatchParam param = magicCodeDispatchparam(model);
            // act
            renderer.render(param);
            // assert
            assertThat(model).containsExactlyInAnyOrderEntriesOf(originalModel);
        }

        @Test
        @DisplayName("throws when email dispatch parameter is null")
        void throwsWhenEmailDispatchParamIsNull() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> renderer.render(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("email dispatch param is required");
        }

        private void assertRendered(RenderedEmailResult result, String subject) {
            assertThat(result.getSubject()).isEqualTo(subject);
            assertThat(result.getHtmlBody()).isNotBlank();
            assertThat(result.getTextBody()).isNotBlank();
        }

    }

}
