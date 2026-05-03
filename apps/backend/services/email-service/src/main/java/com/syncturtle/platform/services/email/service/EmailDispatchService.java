package com.syncturtle.platform.services.email.service;

import java.nio.charset.StandardCharsets;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.syncturtle.platform.services.email.dto.EmailEnvelope;
import com.syncturtle.platform.services.email.dto.EmailRuntimeConfig;
import com.syncturtle.platform.services.email.exceptions.EmailDispatchException;
import com.syncturtle.platform.services.email.service.EmailTemplateService.RenderedEmail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDispatchService {

    private final EmailRuntimeConfigService emailRuntimeConfigService;
    private final DynamicMailSenderFactory dynamicMailSenderFactory;
    private final EmailTemplateService emailTemplateService;

    public void send(EmailEnvelope envelope) {
        EmailRuntimeConfig config = emailRuntimeConfigService.getCurrentConfig();

        if (!config.isEnabled()) {
            throw EmailDispatchException.retryable("SMTP is disabled", null);
        }
        if (!config.isComplete()) {
            throw EmailDispatchException.retryable("SMTP config is incomplete", null);
        }

        RenderedEmail rendered;
        try {
            rendered = emailTemplateService.render(envelope);
        } catch (UnsupportedOperationException | IllegalArgumentException exception) {
            throw EmailDispatchException.permanent("Email template rendering failed", exception);
        }

        JavaMailSender sender = dynamicMailSenderFactory.create(config);

        try {
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            helper.setFrom(config.getFrom());
            helper.setTo(envelope.getTo().toArray(String[]::new));
            helper.setSubject(rendered.subject());
            helper.setText(rendered.textBody(), rendered.htmlBody());

            sender.send(mimeMessage);

            log.info("Email sent successfully. templateType={}, recipients={}, correlationId={}",
                    envelope.getTemplateType(), envelope.getTo(), envelope.getCorrelationId());
        } catch (MailAuthenticationException exception) {
            emailRuntimeConfigService.evict();
            throw EmailDispatchException.retryable("SMTP authentication failed", exception);
        } catch (MailSendException exception) {
            throw EmailDispatchException.retryable("Failed to send email", exception);
        } catch (MessagingException exception) {
            throw EmailDispatchException.permanent("Failed to build email message", exception);
        }
    }

}
