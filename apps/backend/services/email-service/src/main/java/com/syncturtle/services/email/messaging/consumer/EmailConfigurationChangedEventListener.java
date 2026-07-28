package com.syncturtle.services.email.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.email.service.EmailRuntimeConfigChangeService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmailConfigurationChangedEventListener {

    private final EmailRuntimeConfigChangeService service;

    @KafkaListener(topics = KafkaTopics.INSTANCE_CONFIG_EVENTS_V1, groupId = "${app.kafka.config-broadcast-group}", containerFactory = "instanceConfigurationKafkaListenerFactory")
    public void onEmailConfigurationChanged(InstanceConfigurationEvent event) {
        service.receive(event);
    }

}
