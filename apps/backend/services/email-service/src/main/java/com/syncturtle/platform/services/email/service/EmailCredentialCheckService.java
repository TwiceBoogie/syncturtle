package com.syncturtle.platform.services.email.service;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.syncturtle.platform.services.email.dto.EmailRuntimeConfig;
import com.syncturtle.platform.services.email.exceptions.EmailCredentialCheckException;

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
            throw new EmailCredentialCheckException(HttpStatus.BAD_REQUEST, "SMTP is disabled.");
        }
        if (!config.isComplete()) {
            throw new EmailCredentialCheckException(HttpStatus.BAD_REQUEST,
                    "Could not send email. Please check your configuration.");
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
            throw new EmailCredentialCheckException(HttpStatus.BAD_REQUEST, "Invalid credentials provided", exception);
        } catch (MailSendException exception) {
            throw translateMailSendException(exception);
        } catch (MessagingException exception) {
            throw translateMessagingException(exception);
        } catch (Exception exception) {
            throw new EmailCredentialCheckException(HttpStatus.BAD_REQUEST,
                    "Could not send email. Please check your configuration.", exception);
        }
    }

    private EmailCredentialCheckException translateMailSendException(MailSendException exception) {
        Throwable root = rootCause(exception);

        if (root instanceof AuthenticationFailedException) {
            emailRuntimeConfigService.evict();
            return new EmailCredentialCheckException(HttpStatus.BAD_REQUEST, "Invalid credentials provided", exception);
        }

        if (root instanceof SocketTimeoutException || root instanceof TimeoutException) {
            return new EmailCredentialCheckException(HttpStatus.BAD_REQUEST,
                    "Timeout error while trying to connect to the SMTP server", exception);
        }

        if (root instanceof UnknownHostException || root instanceof ConnectException
                || root instanceof NoRouteToHostException) {
            return new EmailCredentialCheckException(HttpStatus.BAD_REQUEST, "Could not connect with the SMTP server.",
                    exception);
        }

        if (root instanceof SendFailedException) {
            return new EmailCredentialCheckException(HttpStatus.BAD_REQUEST, "All recipient addresses were refused.",
                    exception);
        }

        return new EmailCredentialCheckException(HttpStatus.BAD_REQUEST,
                "Could not send email. Plesase check your configuration.", exception);
    }

    private EmailCredentialCheckException translateMessagingException(MessagingException exception) {
        Throwable root = rootCause(exception);

        if (root instanceof AddressException) {
            return new EmailCredentialCheckException(HttpStatus.BAD_REQUEST, "From address is invalid.", exception);
        }

        if (root instanceof SocketTimeoutException || root instanceof TimeoutException) {
            return new EmailCredentialCheckException(HttpStatus.BAD_REQUEST,
                    "Timeout error while trying to connect to the SMTP server.", exception);
        }

        if (root instanceof UnknownHostException || root instanceof ConnectException
                || root instanceof NoRouteToHostException) {
            return new EmailCredentialCheckException(HttpStatus.BAD_REQUEST, "Could not connect with the SMTP server.",
                    exception);
        }

        return new EmailCredentialCheckException(HttpStatus.BAD_REQUEST,
                "Could not send email. Please check your configuration.", exception);
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

}
