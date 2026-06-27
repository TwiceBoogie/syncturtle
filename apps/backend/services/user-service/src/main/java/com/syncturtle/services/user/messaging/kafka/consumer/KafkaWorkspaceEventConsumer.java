package com.syncturtle.services.user.messaging.kafka.consumer;

import java.time.Clock;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.services.user.messaging.kafka.mapper.WorkspaceEventMapper;
import com.syncturtle.services.user.model.WorkspaceLite;
import com.syncturtle.services.user.model.param.WorkspaceLiteReplicaParam;
import com.syncturtle.services.user.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaWorkspaceEventConsumer {

    private final WorkspaceRepository repository;
    private final WorkspaceEventMapper mapper;
    private final Clock clock;

    @Transactional
    @KafkaListener(topics = KafkaTopics.WORKSPACE_EVENTS_V1, groupId = "user-svc-workspace-event-v1", containerFactory = "workspaceKafkaListenerFactory")
    public void onWorkspace(WorkspaceEvent event) {
        Assert.notNull(event, "workspace event is required");

        WorkspaceLiteReplicaParam param = mapper.toParam(event);

        repository.findById(param.getId()).ifPresentOrElse(existing -> applyToExisting(existing, param),
                () -> insertNew(param));
    }

    private void applyToExisting(WorkspaceLite existing, WorkspaceLiteReplicaParam param) {
        boolean changed = existing.applyReplicaParam(param, clock);

        if (!changed) {
            log.debug(
                    "Ignored stale or duplicate workspace replica. workspaceId={} incomingVersion={} currentVersion={}",
                    param.getId(), param.getSourceVersion(), existing.getSourceVersion());
            return;
        }

        repository.save(existing);
    }

    private void insertNew(WorkspaceLiteReplicaParam param) {
        WorkspaceLite workspace = WorkspaceLite.fromReplicaParam(param, clock);

        repository.save(workspace);
    }

}
