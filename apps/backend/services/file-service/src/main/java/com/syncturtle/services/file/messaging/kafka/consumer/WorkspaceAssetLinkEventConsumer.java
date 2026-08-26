package com.syncturtle.services.file.messaging.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.services.file.service.FileAssetLinkSynchronizationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceAssetLinkEventConsumer {

    private final FileAssetLinkSynchronizationService synchronizationService;

    @KafkaListener(topics = KafkaTopics.WORKSPACE_EVENTS_V1, groupId = "${app.file.kafka.workspace-asset-links-group-id}", containerFactory = "workspaceKafkaListenerFactory")
    public void onWorkspaceEvent(WorkspaceEvent event) {
        Assert.notNull(event, "workspace event is required");

        synchronizationService.synchronizeWorkspaceAssetLinks(event);
    }

}
