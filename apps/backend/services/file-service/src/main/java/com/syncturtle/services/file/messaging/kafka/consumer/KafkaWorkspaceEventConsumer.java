package com.syncturtle.services.file.messaging.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.services.file.service.FileAssetLinkService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaWorkspaceEventConsumer {

    private final FileAssetLinkService linkService;

    @Transactional
    @KafkaListener(topics = KafkaTopics.WORKSPACE_EVENTS_V1, groupId = "file-svc-workspace-event-v1", containerFactory = "workspaceKafkaListenerFactory")
    public void onWorkspaceEvent(WorkspaceEvent event) {
        Assert.notNull(event, "workspace event is required");

        if (event.isDeleteEvent()) {
            linkService.removePrimaryWorkspaceLogoLink(event.getId(), event.getVersion());
            return;
        }
        if (event.getLogoAssetId() == null) {
            linkService.removePrimaryWorkspaceLogoLink(event.getId(), event.getVersion());
        } else {
            linkService.upsertPrimaryWorkspaceLogoLink(event);
        }
    }

}
