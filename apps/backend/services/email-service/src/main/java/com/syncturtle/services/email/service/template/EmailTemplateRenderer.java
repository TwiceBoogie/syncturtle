package com.syncturtle.services.email.service.template;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.syncturtle.common.contracts.email.template.EmailTemplateType;
import com.syncturtle.services.email.exception.EmailTemplateException;
import com.syncturtle.services.email.service.param.EmailDispatchParam;
import com.syncturtle.services.email.service.result.RenderedEmailResult;

@Component
public class EmailTemplateRenderer {

    private final TemplateEngine emailTemplateEngine;

    public EmailTemplateRenderer(@Qualifier("emailTemplateEngine") TemplateEngine emailTemplateEngine) {
        Assert.notNull(emailTemplateEngine, "emailTemplateEngine is required");

        this.emailTemplateEngine = emailTemplateEngine;
    }

    public RenderedEmailResult render(EmailDispatchParam param) {
        Assert.notNull(param, "email dispatch param is required");

        EmailTemplateSet templateSet = resolveTemplateSet(param.getTemplateType());

        Context context = new Context();
        context.setVariables(param.getModel());

        try {
            String htmlBody = emailTemplateEngine.process(templateSet.htmlTemplate, context);
            String textBody = emailTemplateEngine.process(templateSet.textTemplate, context);

            return new RenderedEmailResult(param.getSubject(), htmlBody, textBody);
        } catch (Exception exception) {
            throw EmailTemplateException.renderFailed(param.getTemplateType(), exception);
        }
    }

    private EmailTemplateSet resolveTemplateSet(EmailTemplateType templateType) {
        Assert.notNull(templateType, "templateType is required");

        return switch (templateType) {
            case MAGIC_CODE -> new EmailTemplateSet("email/html/magic-code", "email/text/magic-code");
            case PASSWORD_RESET -> new EmailTemplateSet("email/html/forgot-password", "email/text/forgot-password");
            case WORKSPACE_INVITATION ->
                new EmailTemplateSet("email/html/workspace-invitation", "email/text/workspace-invitation");
            case VERIFY_EMAIL,
                    GENERIC_HTML ->
                throw EmailTemplateException.unsupportedTemplate(templateType);
        };
    }

    private static final class EmailTemplateSet {
        private final String htmlTemplate;
        private final String textTemplate;

        private EmailTemplateSet(String htmlTemplate, String textTemplate) {
            Assert.hasText(htmlTemplate, "htmlTemplate is required");
            Assert.hasText(textTemplate, "textTemplate is required");

            this.htmlTemplate = htmlTemplate;
            this.textTemplate = textTemplate;
        }
    }

}
