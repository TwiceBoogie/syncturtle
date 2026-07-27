package com.syncturtle.services.email.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.syncturtle.services.email.exception.EmailCredentialCheckException;
import com.syncturtle.services.email.exception.EmailTransportException;
import com.syncturtle.services.email.service.EmailCredentialCheckService;
import com.syncturtle.services.email.service.param.EmailTransportSendParam;
import com.syncturtle.services.email.service.runtime.EmailRuntimeConfigResolver;
import com.syncturtle.services.email.service.runtime.EmailRuntimeConfigSnapshot;
import com.syncturtle.services.email.service.transport.EmailTransportSender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailCredentialCheckServiceImpl implements EmailCredentialCheckService {

    private static final String TEST_EMAIL_SUBJECT = "Email notification from Syncturtle";
    private static final String TEST_EMAIL_BODY = "This is a sample email notification sent from Syncturtle application.";

    private final EmailRuntimeConfigResolver runtimeConfigResolver;
    private final EmailTransportSender transportSender;

    @Override
    public void sendTestEmail(String receiverEmail) {
        Assert.hasText(receiverEmail, "receiverEmail is required");

        String normalizedReceiverEmail = receiverEmail.trim();

        EmailRuntimeConfigSnapshot config = runtimeConfigResolver.resolveCurrent();
        if (!config.isEnabled()) {
            throw EmailCredentialCheckException.smtpDisabled();
        }

        if (!config.isComplete()) {
            throw EmailCredentialCheckException.smtpNotConfigured();
        }

        EmailTransportSendParam param = EmailTransportSendParam.builder()
                .config(config)
                .recipients(List.of(normalizedReceiverEmail))
                .subject(TEST_EMAIL_SUBJECT)
                .textBody(TEST_EMAIL_BODY)
                .build();

        try {
            transportSender.send(param);
        } catch (EmailTransportException exception) {
            throw translateTransportException(exception);
        }
    }

    private EmailCredentialCheckException translateTransportException(EmailTransportException exception) {
        Assert.notNull(exception, "email transport exception is required");

        return switch (exception.getEmailErrorCode()) {
            case EMAIL_SMTP_AUTHENTICATION_FAILED -> {
                runtimeConfigResolver.evict();

                yield EmailCredentialCheckException.authenticationFailed(exception);
            }
            case EMAIL_SMTP_TIMEOUT -> EmailCredentialCheckException.timeout(exception);
            case EMAIL_SMTP_CONNECTION_FAILED -> EmailCredentialCheckException.connectionFailed(exception);
            case EMAIL_SMTP_RECIPIENTS_REFUSED -> EmailCredentialCheckException.recipientsRefused(exception);
            case EMAIL_SMTP_INVALID_FROM_ADDRESS -> EmailCredentialCheckException.invalidFromAddress(exception);
            case EMAIL_MESSAGE_BUILD_FAILED -> EmailCredentialCheckException.sendFailed(exception);
            default -> EmailCredentialCheckException.sendFailed(exception);
        };
    }

}
