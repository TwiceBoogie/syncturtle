package com.syncturtle.services.user.messaging.kafka.factory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;

@Component
public class MagicLinkEmailEventFactory {

    public EmailToSendEvent magicLink(String email, String token) {
        Assert.hasText(email, "email is required");
        Assert.hasText(token, "token is required");

        return EmailToSendEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now())
                .correlationId(null)
                .templateType(EmailTemplateType.MAGIC_LINK)
                .subject("Your unique Syncturtle login code is " + token)
                .to(List.of(email))
                .model(Map.of("code", token, "email", email))
                .build();
    }

}
