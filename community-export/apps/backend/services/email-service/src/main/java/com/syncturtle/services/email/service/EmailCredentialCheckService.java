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

import com.syncturtle.services.email.dto.EmailRuntimeConfig;
import com.syncturtle.services.email.exceptions.EmailCredentialCheckException;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailCredentialCheckService {

    private final EmailRuntimeConfigService emailRuntimeConfigService;
    private final DynamicMailSenderFactory dynamicMailSenderFactory;

    public void sendTestEmail(String receiverEmail) {
        EmailRuntimeConfig config = emailRuntimeConfigService.getCurrentConfig();

        if (!config.isEnabled()) {
            throw EmailCredentialCheckException.smtpDisabled();
        }
        if (!config.isComplete()) {
            throw EmailCredentialCheckException.smtpNotConfigured();
        }

        JavaMailSender sender = dynamicMailSenderFactory.create(config);

        try {
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(config.getFrom());
            helper.setTo(receiverEmail);
            helper.setSubject("Email notification from Syncturtle");
            helper.setText("This is a sample email notification sent from Syncturtle application.", false);

            sender.send(mimeMessage);
        } catch (MailAuthenticationException exception) {
            emailRuntimeConfigService.evict();
            throw EmailCredentialCheckException.authenticationFailed(exception);
        } catch (MailSendException exception) {
            throw translateMailSendException(exception);
        } catch (MessagingException exception) {
            throw translateMessagingException(exception);
        } catch (Exception exception) {
            throw EmailCredentialCheckException.sendFailed(exception);
        }
    }

    private EmailCredentialCheckException translateMailSendException(MailSendException exception) {
        Throwable root = rootCause(exception);

        if (root instanceof AuthenticationFailedException) {
            emailRuntimeConfigService.evict();
            return EmailCredentialCheckException.authenticationFailed(root);
        }

        if (root instanceof SocketTimeoutException || root instanceof TimeoutException) {
            return EmailCredentialCheckException.timeout(root);
        }

        if (root instanceof UnknownHostException || root instanceof ConnectException
                || root instanceof NoRouteToHostException) {
            return EmailCredentialCheckException.connectionFailed(root);
        }

        if (root instanceof SendFailedException) {
            return EmailCredentialCheckException.recipientsRefused(root);
        }

        return EmailCredentialCheckException.sendFailed(root);
    }

    private EmailCredentialCheckException translateMessagingException(MessagingException exception) {
        Throwable root = rootCause(exception);

        if (root instanceof AddressException) {
            return EmailCredentialCheckException.invalidFromAddress(root);
        }

        if (root instanceof SocketTimeoutException || root instanceof TimeoutException) {
            return EmailCredentialCheckException.timeout(root);
        }

        if (root instanceof UnknownHostException || root instanceof ConnectException
                || root instanceof NoRouteToHostException) {
            return EmailCredentialCheckException.connectionFailed(root);
        }

        return EmailCredentialCheckException.sendFailed(root);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

}
