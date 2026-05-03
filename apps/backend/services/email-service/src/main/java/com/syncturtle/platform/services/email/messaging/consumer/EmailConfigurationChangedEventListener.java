package com.syncturtle.platform.services.email.messaging.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.syncturtle.common.core.constants.KafkaTopicConstants;
import com.syncturtle.common.core.enums.InstanceConfigScope;
import com.syncturtle.common.core.events.InstanceConfigurationEvent;
import com.syncturtle.platform.services.email.service.EmailRuntimeConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConfigurationChangedEventListener {

    private final EmailRuntimeConfigService emailRuntimeConfigService;

    @KafkaListener(topics = KafkaTopicConstants.INSTANCE_CONFIG_EVENTS_V1, groupId = "${app.kafka.consumer-group}")
    public void onEmailConfigurationChanged(InstanceConfigurationEvent event) {
        if (event.getScope() != InstanceConfigScope.EMAIL) {
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
