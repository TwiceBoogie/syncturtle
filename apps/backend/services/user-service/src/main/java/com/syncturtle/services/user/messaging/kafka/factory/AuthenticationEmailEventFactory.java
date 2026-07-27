package com.syncturtle.services.user.messaging.kafka.factory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.email.template.EmailTemplateType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticationEmailEventFactory {

    private final Clock clock;

    public EmailToSendEvent magicCode(String email, String code, Duration expiresIn) {
        Assert.hasText(email, "email is required");
        Assert.hasText(code, "code is required");
        requirePositive(expiresIn, "expiresIn");

        long expiresInMinutes = expiresIn.toMinutes();

        return EmailToSendEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now(clock))
                .templateType(EmailTemplateType.MAGIC_CODE)
                .subject("Your unique Syncturtle login code is " + code)
                .to(List.of(email))
                .model(Map.of("code", code, "email", email, "expiresInMinutes", expiresInMinutes))
                .build();
    }

    public EmailToSendEvent passwordReset(String email, String resetUrl, Duration expiresIn) {
        Assert.hasText(email, "email is required");
        Assert.hasText(resetUrl, "resetUrl is required");
        requirePositive(expiresIn, "expiresIn");

        long expiresInMinutes = expiresIn.toMinutes();

        return EmailToSendEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now(clock))
                .templateType(EmailTemplateType.PASSWORD_RESET)
                .subject(resetUrl)
                .to(List.of(email))
                .model(Map.of("email", email, "resetUrl", resetUrl, "expiresIn", expiresInMinutes))
                .build();
    }

    private void requirePositive(Duration duration, String fieldName) {
        Assert.notNull(duration, fieldName + " is required");
        Assert.isTrue(!duration.isZero() && !duration.isNegative(), fieldName + " must be positive");
    }

}
