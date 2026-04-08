package com.syncturtle.platform.services.email.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.syncturtle.platform.services.email.dto.EmailEnvelope;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    public RenderedEmail render(EmailEnvelope envelope) {
        return switch (envelope.getTemplateType()) {
            case MAGIC_LINK -> renderMagicLink(envelope);
            case PASSWORD_RESET -> throw new UnsupportedOperationException("PASSWORD_RESET not implemented yet");
            case VERIFY_EMAIL -> throw new UnsupportedOperationException("VERIFY_EMAIL not implemented yet");
            case GENERIC_HTML -> throw new UnsupportedOperationException("GENERIC_HTML not implemented yet");
        };
    }

    private RenderedEmail renderMagicLink(EmailEnvelope envelope) {
        Context htmlContext = createContext(envelope.getModel());
        Context textContext = createContext(envelope.getModel());

        String html = templateEngine.process("email/magic-link", htmlContext);
        String text = templateEngine.process("email/magic-link.txt", textContext);

        return new RenderedEmail(envelope.getSubject(), html, text);
    }

    private Context createContext(Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);
        return context;
    }

    public record RenderedEmail(String subject, String htmlBody, String textBody) {
    }

}
