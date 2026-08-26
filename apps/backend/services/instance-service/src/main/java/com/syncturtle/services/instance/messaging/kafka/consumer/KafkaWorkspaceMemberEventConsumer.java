package com.syncturtle.services.instance.messaging.kafka.consumer;

import java.time.Clock;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.services.instance.messaging.kafka.mapper.WorkspaceMemberEventMapper;
import com.syncturtle.services.instance.model.WorkspaceMemberLite;
import com.syncturtle.services.instance.model.param.WorkspaceMemberLiteReplicaParam;
import com.syncturtle.services.instance.repository.WorkspaceMemberLiteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaWorkspaceMemberEventConsumer {

    private final WorkspaceMemberLiteRepository repository;
    private final WorkspaceMemberEventMapper mapper;
    private final Clock clock;

    @Transactional
    @KafkaListener(topics = KafkaTopics.WORKSPACE_MEMBER_EVENTS_V1, groupId = "instance-svc-workspace-member-event-v1", containerFactory = "workspaceMemberKafkaListenerFactory")
    public void onWorkspaceMemberEvent(WorkspaceMemberEvent event) {
        Assert.notNull(event, "workspace member event is required");

        WorkspaceMemberLiteReplicaParam param = mapper.toParam(event);

        repository.findById(param.getId()).ifPresentOrElse(existing -> applyToExisting(existing, param),
                () -> insertNew(param));
    }

    private void applyToExisting(WorkspaceMemberLite existing, WorkspaceMemberLiteReplicaParam param) {
        boolean changed = existing.applyReplicaParam(param, clock);

        if (!changed) {
            log.debug(
                    "Ignored stale or duplicate workspace member replica. workspaceMemberId={} incomingVersion={} currentVersion={}",
                    param.getId(), param.getSourceVersion(), existing.getSourceVersion());
            return;
        }

        repository.save(existing);
    }

    private void insertNew(WorkspaceMemberLiteReplicaParam param) {
        WorkspaceMemberLite member = WorkspaceMemberLite.fromReplicaParam(param, clock);

        repository.save(member);
    }

}
