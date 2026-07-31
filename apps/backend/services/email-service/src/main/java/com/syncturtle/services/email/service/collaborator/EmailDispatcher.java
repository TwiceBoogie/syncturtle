package com.syncturtle.services.email.service.collaborator;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.services.email.exception.EmailDispatchException;
import com.syncturtle.services.email.exception.EmailTemplateException;
import com.syncturtle.services.email.exception.EmailTransportException;
import com.syncturtle.services.email.service.param.EmailDispatchParam;
import com.syncturtle.services.email.service.param.EmailTransportSendParam;
import com.syncturtle.services.email.service.result.RenderedEmailResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDispatcher {

    private final EmailRuntimeConfigResolver runtimeConfigResolver;
    private final EmailTemplateRenderer templateRenderer;
    private final EmailTransportSender transportSender;

    public void dispatch(EmailDispatchParam param) {
        Assert.notNull(param, "email dispatch param is required");

        EmailRuntimeConfigSnapshot config = runtimeConfigResolver.resolveCurrent();
        if (!config.isEnabled()) {
            throw EmailDispatchException.smtpDisabled();
        }

        if (!config.isComplete()) {
            throw EmailDispatchException.smtpNotConfigured();
        }

        RenderedEmailResult renderedEmail;

        try {
            renderedEmail = templateRenderer.render(param);
        } catch (EmailTemplateException exception) {
            throw EmailDispatchException.templateFailed(exception);
        }

        EmailTransportSendParam transportParam = EmailTransportSendParam.builder()
                .config(config)
                .recipients(param.getRecipients())
                .subject(renderedEmail.getSubject())
                .textBody(renderedEmail.getTextBody())
                .htmlBody(renderedEmail.getHtmlBody())
                .build();

        try {
            transportSender.send(transportParam);
        } catch (EmailTransportException exception) {
            throw translateTransportException(exception);
        }

        log.info("Email sent successfully. templateType={}, recipients={}, eventId={}", param.getTemplateType(),
                param.getRecipients(), param.getEventId());
    }

    private EmailDispatchException translateTransportException(EmailTransportException exception) {
        Assert.notNull(exception, "email transport exception is required");

        return switch (exception.getEmailErrorCode()) {
            case EMAIL_SMTP_AUTHENTICATION_FAILED -> {
                runtimeConfigResolver.evict();

                yield EmailDispatchException.authenticationFailed(exception);
            }
            case EMAIL_SMTP_TIMEOUT -> EmailDispatchException.timeout(exception);
            case EMAIL_SMTP_CONNECTION_FAILED -> EmailDispatchException.connectionFailed(exception);
            case EMAIL_SMTP_RECIPIENTS_REFUSED -> EmailDispatchException.recipientsRefused(exception);
            case EMAIL_SMTP_INVALID_FROM_ADDRESS, EMAIL_MESSAGE_BUILD_FAILED ->
                EmailDispatchException.messageBuildFailed(exception);
            case EMAIL_SMTP_SEND_FAILED -> EmailDispatchException.sendFailed(exception);
            default -> EmailDispatchException.dispatchFailed();
        };
    }

}
