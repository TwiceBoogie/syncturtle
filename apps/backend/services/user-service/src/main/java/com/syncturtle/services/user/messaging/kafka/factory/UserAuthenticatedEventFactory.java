package com.syncturtle.services.user.messaging.kafka.factory;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.user.event.UserAuthenticatedEvent;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.type.CredentialProviderType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAuthenticatedEventFactory {

    private final Clock clock;

    public UserAuthenticatedEvent authenticated(User user, boolean createdUser, CredentialProviderType provider) {
        Assert.notNull(user, "user is required");
        Assert.notNull(user.getId(), "user id is required");
        Assert.hasText(user.getEmail(), "user email is required");
        Assert.notNull(provider, "credential provider is required");

        return UserAuthenticatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now(clock))
                .userId(user.getId())
                .email(user.getEmail())
                .createdUser(createdUser)
                .provider(provider.value())
                .build();
    }

}
