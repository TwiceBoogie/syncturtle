package com.syncturtle.services.email.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScopeNames;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.email.service.EmailRuntimeConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConfigurationChangedEventListener {

    private final EmailRuntimeConfigService emailRuntimeConfigService;

    @KafkaListener(topics = KafkaTopics.INSTANCE_CONFIG_EVENTS_V1, groupId = "${app.kafka.config-broadcast-group}", containerFactory = "instanceConfigurationKafkaListenerFactory")
    public void onEmailConfigurationChanged(InstanceConfigurationEvent event) {
        if (event.getScope() != InstanceConfigurationScopeNames.EMAIL) {
            log.debug("Ignoring instance config event for non-email scope. scope={}, eventId={}", event.getScope(),
                    event.getEventId());
            return;
        }

        emailRuntimeConfigService.refreshIfOlderThan(event.getGlobalVersion());

        log.info("Email runtime config refreshed. scope={}, scopeVersion={}, globalVersion={}, eventId={}",
                event.getScope(),
                event.getScopeVersion(),
                event.getGlobalVersion(),
                event.getEventId());
    }

}
