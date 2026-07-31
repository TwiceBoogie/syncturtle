package com.syncturtle.services.user.messaging.kafka.consumer;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeSecretResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(prefix = "app.kafka", name = "enabled")
public class UserAuthSecretConfigurationChangedEventListener {

    private static final Set<InstanceConfigurationKey> SECRET_KEYS = EnumSet.of(
            InstanceConfigurationKey.GOOGLE_CLIENT_ID,
            InstanceConfigurationKey.GOOGLE_CLIENT_SECRET,
            InstanceConfigurationKey.GITHUB_CLIENT_ID,
            InstanceConfigurationKey.GITHUB_CLIENT_SECRET,
            InstanceConfigurationKey.GITHUB_APP_NAME,
            InstanceConfigurationKey.GITLAB_HOST,
            InstanceConfigurationKey.GITLAB_CLIENT_ID,
            InstanceConfigurationKey.GITLAB_CLIENT_SECRET);

    private final UserAuthRuntimeSecretResolver resolver;

    @KafkaListener(topics = KafkaTopics.INSTANCE_CONFIG_EVENTS_V1, groupId = "#{@instanceConfigurationConsumerGroups.localGroupId}", containerFactory = "instanceConfigurationKafkaListenerFactory")
    public void onInstanceConfigurationChanged(InstanceConfigurationEvent event) {
        if (event.getScope() != InstanceConfigurationScope.AUTH) {
            return;
        }
        if (event.affectsAny(SECRET_KEYS)) {
            resolver.refreshIfOlderThan(event.getConfigurationVersion());
        }
    }
}
