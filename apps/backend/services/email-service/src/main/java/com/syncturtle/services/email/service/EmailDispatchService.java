package com.syncturtle.services.email.service;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.syncturtle.services.email.dto.EmailEnvelope;
import com.syncturtle.services.email.dto.EmailRuntimeConfig;
import com.syncturtle.services.email.exception.EmailDispatchException;
import com.syncturtle.services.email.exception.EmailTemplateException;
import com.syncturtle.services.email.service.EmailTemplateService.RenderedEmail;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
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
            throw EmailDispatchException.smtpDisabled();
        }
        if (!config.isComplete()) {
            throw EmailDispatchException.smtpNotConfigured();
        }

        RenderedEmail rendered;
        try {
            rendered = emailTemplateService.render(envelope);
        } catch (EmailTemplateException exception) {
            throw EmailDispatchException.templateFailed(exception);
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
            throw EmailDispatchException.authenticationFailed(exception);
        } catch (MailSendException exception) {
            throw translateMailSendException(exception);
        } catch (MessagingException exception) {
            throw EmailDispatchException.messageBuildFailed(exception);
        }
    }

    private EmailDispatchException translateMailSendException(MailSendException exception) {
        Throwable root = rootCause(exception);

        if (root instanceof AuthenticationFailedException) {
            emailRuntimeConfigService.evict();
            return EmailDispatchException.authenticationFailed(exception);
        }

        if (isTimeout(root)) {
            return EmailDispatchException.timeout(exception);
        }

        if (isConnectionFailure(root)) {
            return EmailDispatchException.connectionFailed(exception);
        }

        if (root instanceof SendFailedException) {
            return EmailDispatchException.recipientsRefused(exception);
        }

        return EmailDispatchException.sendFailed(exception);
    }

    private static boolean isTimeout(Throwable throwable) {
        return throwable instanceof SocketTimeoutException || throwable instanceof TimeoutException;
    }

    private static boolean isConnectionFailure(Throwable throwable) {
        return throwable instanceof UnknownHostException || throwable instanceof ConnectException
                || throwable instanceof NoRouteToHostException;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

}
