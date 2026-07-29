package com.syncturtle.services.user.messaging.kafka.consumer;

import java.time.Clock;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberInviteEvent;
import com.syncturtle.services.user.messaging.kafka.mapper.WorkspaceMemberInviteEventMapper;
import com.syncturtle.services.user.model.WorkspaceMemberInviteLite;
import com.syncturtle.services.user.model.param.WorkspaceMemberInviteLiteReplicaParam;
import com.syncturtle.services.user.repository.WorkspaceMemberInviteLiteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaWorkspaceMemberInviteEventConsumer {

    private final WorkspaceMemberInviteLiteRepository repository;
    private final WorkspaceMemberInviteEventMapper mapper;
    private final Clock clock;

    @Transactional
    @KafkaListener(topics = KafkaTopics.WORKSPACE_MEMBER_INVITE_EVENTS_V1, groupId = "user-svc-workspace-member-invite-event-v1", containerFactory = "workspaceMemberInviteKafkaListenerFactory")
    public void onWorkspaceMemberInvite(WorkspaceMemberInviteEvent event) {
        Assert.notNull(event, "workspace member invite is required");

        WorkspaceMemberInviteLiteReplicaParam param = mapper.toParam(event);

        repository.findById(param.getId()).ifPresentOrElse(existing -> applyToExisting(existing, param),
                () -> insertNew(param));
    }

    private void applyToExisting(WorkspaceMemberInviteLite existing, WorkspaceMemberInviteLiteReplicaParam param) {
        boolean changed = existing.applyReplicaParam(param, clock);

        if (!changed) {
            log.debug(
                    "Ignored stale or duplicate workspace member invite replica. inviteId={} incomingVersion={} currentVersion={}",
                    param.getId(), param.getSourceVersion(), existing.getSourceVersion());
            return;
        }

        repository.save(existing);
    }

    private void insertNew(WorkspaceMemberInviteLiteReplicaParam param) {
        WorkspaceMemberInviteLite invite = WorkspaceMemberInviteLite.fromReplicaParam(param, clock);

        repository.save(invite);
    }

}
