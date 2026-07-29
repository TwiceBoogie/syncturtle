package com.syncturtle.services.email.service.collaborator;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.services.email.configuration.property.EmailTransportProperties;
import com.syncturtle.services.email.exception.EmailTransportException;
import com.syncturtle.services.email.service.param.EmailTransportSendParam;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailTransportSender {

    private final EmailTransportProperties properties;

    public void send(EmailTransportSendParam param) {
        Assert.notNull(param, "email transport param is required");

        JavaMailSender sender = createSender(param.getConfig());

        try {
            MimeMessage mimeMessage = sender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, param.hasHtmlBody(),
                    StandardCharsets.UTF_8.name());

            helper.setFrom(param.getConfig().getFrom());
            helper.setTo(param.getRecipients().toArray(String[]::new));
            helper.setSubject(param.getSubject());

            if (param.hasHtmlBody()) {
                helper.setText(param.getTextBody(), param.getHtmlBody());
            } else {
                helper.setText(param.getTextBody(), false);
            }

            sender.send(mimeMessage);
        } catch (MailAuthenticationException exception) {
            throw EmailTransportException.authenticationFailed(exception);
        } catch (MailSendException exception) {
            throw translateMailSendException(exception);
        } catch (MessagingException exception) {
            throw translateMessagingException(exception);
        } catch (Exception exception) {
            throw EmailTransportException.sendFailed(exception);
        }
    }

    private JavaMailSender createSender(EmailRuntimeConfigSnapshot config) {
        Assert.notNull(config, "runtime config is required");
        Assert.state(config.isComplete(), "runtime config must be complete");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        sender.setHost(config.getHost());
        sender.setPort(config.getPort());

        if (config.getUsername() != null) {
            sender.setUsername(config.getUsername());
        }

        if (config.getPassword() != null) {
            sender.setPassword(config.getPassword());
        }

        Properties mailProperties = sender.getJavaMailProperties();

        mailProperties.put("mail.transport.protocol", "smtp");
        mailProperties.put("mail.smtp.auth", Boolean.toString(config.requiresAuthentication()));
        mailProperties.put("mail.smtp.starttls.enable", Boolean.toString(config.isUseTls()));
        mailProperties.put("mail.smtp.ssl.enable", Boolean.toString(config.isUseSsl()));
        mailProperties.put("mail.smtp.connectiontimeout", Integer.toString(properties.getConnectionTimeoutMillis()));
        mailProperties.put("mail.smtp.timeout", Integer.toString(properties.getReadTimeoutMillis()));
        mailProperties.put("mail.smtp.writetimeout", Integer.toString(properties.getWriteTimeoutMillis()));

        return sender;
    }

    private EmailTransportException translateMailSendException(MailSendException exception) {
        Throwable root = rootCause(exception);

        if (root instanceof AuthenticationFailedException) {
            return EmailTransportException.authenticationFailed(exception);
        }

        if (isTimeout(root)) {
            return EmailTransportException.timeout(exception);
        }

        if (isConnectionFailure(root)) {
            return EmailTransportException.connectionFailed(exception);
        }

        if (root instanceof SendFailedException) {
            return EmailTransportException.recipientsRefused(exception);
        }

        return EmailTransportException.sendFailed(exception);
    }

    private EmailTransportException translateMessagingException(MessagingException exception) {
        Throwable root = rootCause(exception);

        if (root instanceof AddressException) {
            return EmailTransportException.invalidFromAddress(exception);
        }

        if (isTimeout(root)) {
            return EmailTransportException.timeout(exception);
        }

        if (isConnectionFailure(root)) {
            return EmailTransportException.connectionFailed(exception);
        }

        return EmailTransportException.messageBuildFailed(exception);
    }

    private static boolean isTimeout(Throwable throwable) {
        return throwable instanceof SocketTimeoutException || throwable instanceof TimeoutException;
    }

    private static boolean isConnectionFailure(Throwable throwable) {
        return throwable instanceof UnknownHostException
                || throwable instanceof ConnectException
                || throwable instanceof NoRouteToHostException;
    }

    private static Throwable rootCause(Throwable throwable) {
        Assert.notNull(throwable, "throwable is required");

        Throwable current = throwable;

        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }

        return current;
    }

}
