package com.syncturtle.services.email.service;

import java.util.Properties;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import com.syncturtle.services.email.configurations.properties.SyncturtleConfig;
import com.syncturtle.services.email.dto.EmailRuntimeConfig;

import lombok.RequiredArgsConstructor;

/**
 * We intentionally avoid static ownership here and build
 * {@code JavaMailSenderImpl} ourselves from runtime config
 */
@Component
@RequiredArgsConstructor
public class DynamicMailSenderFactory {

    private final SyncturtleConfig syncturtleConfig;

    public JavaMailSender create(EmailRuntimeConfig config) {
        if (!config.isComplete()) {
            throw new IllegalStateException("Email runtime config is incomplete");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getHost());
        sender.setPort(config.getPort());

        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            sender.setUsername(config.getUsername());
        }
        if (config.getPassword() != null && !config.getPassword().isBlank()) {
            sender.setPassword(config.getPassword());
        }

        Properties mailProps = sender.getJavaMailProperties();
        mailProps.put("mail.transport.protocol", "smtp");
        mailProps.put("mail.smtp.auth", Boolean.toString(config.requiresAuthentication()));
        mailProps.put("mail.smtp.starttls.enable", Boolean.toString(config.isUseTls()));
        mailProps.put("mail.smtp.ssl.enable", Boolean.toString(config.isUseSsl()));
        mailProps.put("mail.smtp.connectiontimeout", syncturtleConfig.getEmail().getSender().getConnectionTimeoutMs());
        mailProps.put("mail.smtp.timeout", syncturtleConfig.getEmail().getSender().getTimeoutMs());
        mailProps.put("mail.smtp.writetimeout", syncturtleConfig.getEmail().getSender().getWriteTimeoutMs());

        return sender;
    }

}
