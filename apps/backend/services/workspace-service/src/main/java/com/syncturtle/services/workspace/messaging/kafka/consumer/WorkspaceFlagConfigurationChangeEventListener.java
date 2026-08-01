package com.syncturtle.services.workspace.messaging.kafka.consumer;

import java.util.Set;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.workspace.service.collaborator.runtime.WorkspaceConfigResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceFlagConfigurationChangeEventListener {

    private static final Set<InstanceConfigurationKey> WORKSPACE_KEYS = Set.of(
            InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION);

    private final WorkspaceConfigResolver configResolver;

    @KafkaListener(topics = KafkaTopics.INSTANCE_CONFIG_EVENTS_V1, groupId = "#{@instanceConfigurationConsumerGroups.sharedGroupId}", containerFactory = "instanceConfigurationKafkaListenerFactory")
    public void onInstanceConfigurationChanged(InstanceConfigurationEvent event) {
        if (event.getScope() != InstanceConfigurationScope.WORKSPACE) {
            return;
        }
        if (event.affectsAny(WORKSPACE_KEYS)) {
            configResolver.evict();
        }
    }

}
