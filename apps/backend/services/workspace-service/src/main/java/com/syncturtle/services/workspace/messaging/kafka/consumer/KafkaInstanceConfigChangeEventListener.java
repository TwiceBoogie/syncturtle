package com.syncturtle.services.workspace.messaging.kafka.consumer;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.workspace.service.collaborator.runtime.WorkspaceConfigResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaInstanceConfigChangeEventListener {

    private static final Set<InstanceConfigurationKey> REDIS_WORKSPACE_KEYS = EnumSet.of(
            InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION);

    private final WorkspaceConfigResolver configResolver;

    @KafkaListener(topics = KafkaTopics.INSTANCE_CONFIG_EVENTS_V1, groupId = "${app.kafka.config-broadcast-group}", containerFactory = "instanceConfigurationKafkaListenerFactory")
    public void onInstanceConfigurationChanged(InstanceConfigurationEvent event) {
        if (event.getScope() != InstanceConfigurationScope.WORKSPACE) {
            log.debug("Ignoring instance config event for non-workspace scope. scope={}, eventId={}",
                    event.getScope(), event.getEventId());
            return;
        }

        Set<InstanceConfigurationKey> changedKeys = event.getChangedKeys();
        boolean shouldEvictAllWorkspaceConfig = changedKeys == null || changedKeys.isEmpty();
        boolean shouldEvictWorkspaceRuntimeConfig = shouldEvictAllWorkspaceConfig
                || intersects(changedKeys, REDIS_WORKSPACE_KEYS);

        if (shouldEvictWorkspaceRuntimeConfig) {
            configResolver.evict();
        }

        log.info(
                "Handled instance config WORKSPACE event. runtimeConfigEvicted={}, changedKeys={}, globalVersion={}, eventId={}",
                shouldEvictWorkspaceRuntimeConfig, changedKeys, event.getGlobalVersion(), event.getEventId());
    }

    private boolean intersects(Set<InstanceConfigurationKey> changed, Set<InstanceConfigurationKey> target) {
        for (InstanceConfigurationKey key : changed) {
            if (target.contains(key)) {
                return true;
            }
        }
        return false;
    }

}
