package com.syncturtle.platform.services.workspace.messaging.kafka.consumer;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.core.constants.KafkaTopicConstants;
import com.syncturtle.common.core.events.UserEvent;
import com.syncturtle.platform.services.workspace.models.User;
import com.syncturtle.platform.services.workspace.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaUserEventConsumer {

    private final UserRepository userRepository;

    @Transactional
    @KafkaListener(topics = KafkaTopicConstants.USER_EVENTS_V1, groupId = "workspace-svc-user-event-v1", properties = {
            "spring.json.value.default.type=com.syncturtle.common.core.events.UserEvent"
    })
    public void onUser(UserEvent event) {
        UUID userId = event.getId();

        userRepository.findById(userId).ifPresentOrElse(existing -> {
            Long current = existing.getVersion();
            Long incoming = event.getVersion();

            // ignore dupe or out of order events
            if (incoming != null && current != null && incoming <= current) {
                return;
            }

            existing.setUsername(event.getUsername());
            existing.setEmail(event.getEmail());
            existing.setDisplayName(event.getDisplayName());
            existing.setFirstName(event.getFirstName());
            existing.setLastName(event.getLastName());
            existing.setAvatarAssetId(event.getAvatarAssetId());
            existing.setCoverImageAssetId(event.getCoverImageAssetId());
            existing.setActive(event.isActive());
            existing.setPasswordAutoset(event.isPasswordAutoset());
            existing.setUserTimezone(event.getUserTimezone());
            existing.setVersion(incoming);
        }, () -> {
            User user = new User();
            user.setId(userId);
            user.setUsername(event.getUsername());
            user.setEmail(event.getEmail());
            user.setDisplayName(event.getDisplayName());
            user.setFirstName(event.getFirstName());
            user.setLastName(event.getLastName());
            user.setDateJoined(event.getDateJoined());
            user.setAvatarAssetId(event.getAvatarAssetId());
            user.setCoverImageAssetId(event.getCoverImageAssetId());
            user.setActive(event.isActive());
            user.setPasswordAutoset(event.isPasswordAutoset());
            user.setUserTimezone(event.getUserTimezone());
            user.setBot(event.isBot());
            user.setVersion(event.getVersion());

            userRepository.save(user);
        });
    }

}
