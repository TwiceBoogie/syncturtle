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
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeConfigResolver;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(prefix = "app.kafka", name = "enabled")
public class UserAuthFlagConfigurationChangedEventListener {

    private static final Set<InstanceConfigurationKey> FLAG_KEYS = EnumSet.of(
            InstanceConfigurationKey.ENABLE_SIGNUP,
            InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN,
            InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD,
            InstanceConfigurationKey.ENABLE_SMTP,
            InstanceConfigurationKey.IS_GOOGLE_ENABLED,
            InstanceConfigurationKey.IS_GITHUB_ENABLED,
            InstanceConfigurationKey.IS_GITLAB_ENABLED);

    private final UserAuthRuntimeConfigResolver resolver;

    @KafkaListener(topics = KafkaTopics.INSTANCE_CONFIG_EVENTS_V1, groupId = "#{@instanceConfigurationConsumerGroups.sharedGroupId}", containerFactory = "instanceConfigurationKafkaListenerFactory")
    public void onInstanceConfigurationChanged(InstanceConfigurationEvent event) {
        if (event.getScope() != InstanceConfigurationScope.AUTH) {
            return;
        }

        if (event.affectsAny(FLAG_KEYS)) {
            resolver.evict();
        }
    }

}
