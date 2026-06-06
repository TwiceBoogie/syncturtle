package com.syncturtle.services.instance.messaging.kafka.consumer;

import java.time.Clock;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.services.instance.messaging.kafka.mapper.WorkspaceEventMapper;
import com.syncturtle.services.instance.model.Workspace;
import com.syncturtle.services.instance.model.param.WorkspaceReplicaParam;
import com.syncturtle.services.instance.repository.WorkspaceRepository;

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
    @KafkaListener(topics = KafkaTopics.WORKSPACE_EVENTS_V1, groupId = "instance-svc-workspace-event-v1", containerFactory = "workspaceKafkaListenerFactory")
    public void onWorkspace(WorkspaceEvent event) {
        Assert.notNull(event, "workspace event is required");

        WorkspaceReplicaParam param = mapper.toParam(event);

        repository.findById(param.getId()).ifPresentOrElse(
                existing -> applyToExisting(existing, param),
                () -> insertNew(param));
    }

    private void applyToExisting(Workspace existing, WorkspaceReplicaParam param) {
        boolean changed = existing.applyReplicaParam(param, clock);

        if (!changed) {
            log.debug(
                    "Ignored stale or uplicate workspace replica. workspaceId={} incomingVersion={} currentVersion={}",
                    param.getId(),
                    param.getSourceVersion(),
                    existing.getVersion());

            return;
        }

        repository.save(existing);
    }

    private void insertNew(WorkspaceReplicaParam param) {
        Workspace workspace = Workspace.fromReplicaParam(param, clock);

        repository.save(workspace);
    }

}
