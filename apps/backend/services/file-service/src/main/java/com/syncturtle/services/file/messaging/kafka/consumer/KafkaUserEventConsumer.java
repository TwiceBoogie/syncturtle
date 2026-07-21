package com.syncturtle.services.file.messaging.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.services.file.service.FileAssetLinkService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUserEventConsumer {

    private final FileAssetLinkService linkService;

    @Transactional
    @KafkaListener(topics = KafkaTopics.USER_EVENTS_V1, groupId = "file-svc-user-event-v1", containerFactory = "userKafkaListenerFactory")
    public void onUserEvent(UserEvent event) {
        Assert.notNull(event, "user event is required");

        if (event.isDeleteEvent()) {
            linkService.removePrimaryUserAvatarLink(event.getId(), event.getVersion());
            linkService.removePrimaryUserCoverLink(event.getId(), event.getVersion());
            return;
        }

        if (event.getAvatarAssetId() == null) {
            linkService.removePrimaryUserAvatarLink(event.getId(), event.getVersion());
        } else {
            linkService.upsertPrimaryUserAvatarLink(event);
        }

        if (event.getCoverImageAssetId() == null) {
            linkService.removePrimaryUserCoverLink(event.getId(), event.getVersion());
        } else {
            linkService.upsertPrimaryUserCoverLink(event);
        }
    }

}
