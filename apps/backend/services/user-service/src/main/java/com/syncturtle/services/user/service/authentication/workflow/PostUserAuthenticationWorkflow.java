package com.syncturtle.services.user.service.authentication.workflow;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.user.event.UserAuthenticatedEvent;
import com.syncturtle.services.user.messaging.kafka.factory.UserAuthenticatedEventFactory;
import com.syncturtle.services.user.messaging.outbox.UserAuthenticationOutboxWriter;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.type.CredentialProviderType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostUserAuthenticationWorkflow {

    private final UserAuthenticatedEventFactory userAuthenticatedEventFactory;
    private final UserAuthenticationOutboxWriter userAuthenticationOutboxWriter;

    @Transactional
    public void afterAuthentication(User user, boolean createdUser, CredentialProviderType provider) {
        Assert.notNull(user, "user is required");
        Assert.notNull(user.getId(), "user id is required");
        Assert.hasText(user.getEmail(), "user email is required");
        Assert.notNull(provider, "credential provider is required");

        UserAuthenticatedEvent event = userAuthenticatedEventFactory.authenticated(user, createdUser, provider);
        userAuthenticationOutboxWriter.saveUserAuthenticatedEvent(event);
    }

}
