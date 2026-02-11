package com.syncturtle.platform.services.user.messaging.kafka.consumer;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.core.constants.KafkaTopicConstants;
import com.syncturtle.common.core.events.InstanceEvent;
import com.syncturtle.platform.services.user.models.Instance;
import com.syncturtle.platform.services.user.repositories.InstanceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class KafkaInstanceEventConsumer {

    private final InstanceRepository instanceRepository;

    @Transactional
    @KafkaListener(topics = KafkaTopicConstants.INSTANCE_EVENTS_V1, groupId = "user-svc-instance-event-v1")
    public void onInstance(InstanceEvent event) {
        UUID instanceId = event.getId();

        instanceRepository.findById(instanceId).ifPresentOrElse(existing -> {
            Long current = existing.getVersion();
            Long incoming = event.getVersion();

            // ignore dupe or out of order events
            if (incoming != null && current != null && incoming <= current) {
                return;
            }

            existing.setEdition(event.getEdition());
            existing.setSetupDone(event.isSetupDone());
            existing.setUpdatedAt(event.getUpdatedAt());
            existing.setVersion(event.getVersion());
        }, () -> {
            Instance instance = new Instance();
            instance.setId(instanceId);
            instance.setEdition(event.getEdition());
            instance.setSetupDone(event.isSetupDone());
            instance.setUpdatedAt(event.getUpdatedAt());
            instance.setCreatedAt(event.getCreatedAt());
            instance.setVersion(event.getVersion());

            instanceRepository.save(instance);
        });
    }

}
