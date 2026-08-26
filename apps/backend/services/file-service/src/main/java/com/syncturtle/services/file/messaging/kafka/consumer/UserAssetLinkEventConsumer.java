package com.syncturtle.services.file.messaging.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.services.file.service.FileAssetLinkSynchronizationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserAssetLinkEventConsumer {

    private final FileAssetLinkSynchronizationService synchronizationService;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS_V1, groupId = "${app.file.kafka.user-asset-links-group-id}", containerFactory = "userKafkaListenerFactory")
    public void onUserEvent(UserEvent event) {
        Assert.notNull(event, "user event is required");

        synchronizationService.synchronizeUserAssetLinks(event);
    }

}
