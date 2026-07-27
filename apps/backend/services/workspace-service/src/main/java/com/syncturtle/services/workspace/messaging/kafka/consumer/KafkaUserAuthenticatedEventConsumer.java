package com.syncturtle.services.workspace.messaging.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.user.event.UserAuthenticatedEvent;
import com.syncturtle.services.workspace.service.AcceptedWorkspaceInvitationCompletionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUserAuthenticatedEventConsumer {

    private final AcceptedWorkspaceInvitationCompletionService completionService;

    @KafkaListener(topics = KafkaTopics.USER_AUTHENTICATED_EVENTS_V1, groupId = "workspace-svc-user-authenticated-event-v1", containerFactory = "userAuthenticatedKafkaListenerFactory")
    public void onCommand(UserAuthenticatedEvent event) {
        Assert.notNull(event, "workspace membership command is required");
        Assert.notNull(event.getUserId(), "userId is required");
        Assert.hasText(event.getEmail(), "email is required");

        log.debug(
                "Completing accepted workspace invitations after authentication. userId={} email={} createdUser={} provider={}",
                event.getUserId(), event.getEmail(), event.isCreatedUser(), event.getProvider());

        completionService.completeAcceptedInvitations(event.getUserId(), event.getEmail());
    }

}
