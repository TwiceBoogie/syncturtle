package com.syncturtle.services.user.messaging.kafka.consumer;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScopeNames;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.user.services.FeatureFlagService;
import com.syncturtle.services.user.services.UserAuthRuntimeSecretService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaInstanceConfigChangedEventListener {

    private static final Set<InstanceConfigurationKey> REDIS_AUTH_KEYS = EnumSet.of(
            InstanceConfigurationKey.ENABLE_SIGNUP,
            InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN,
            InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD,
            InstanceConfigurationKey.ENABLE_SMTP,
            InstanceConfigurationKey.IS_GOOGLE_ENABLED,
            InstanceConfigurationKey.IS_GITHUB_ENABLED,
            InstanceConfigurationKey.IS_GITLAB_ENABLED);

    private static final Set<InstanceConfigurationKey> CAFFEINE_AUTH_SECRET_KEYS = EnumSet.of(
            InstanceConfigurationKey.GOOGLE_CLIENT_ID,
            InstanceConfigurationKey.GOOGLE_CLIENT_SECRET,
            InstanceConfigurationKey.GITHUB_CLIENT_ID,
            InstanceConfigurationKey.GITHUB_CLIENT_SECRET,
            InstanceConfigurationKey.GITHUB_APP_NAME,
            InstanceConfigurationKey.GITLAB_HOST,
            InstanceConfigurationKey.GITLAB_CLIENT_ID,
            InstanceConfigurationKey.GITLAB_CLIENT_SECRET);

    private final FeatureFlagService featureFlagService;
    private final UserAuthRuntimeSecretService userAuthRuntimeSecretService;

    @KafkaListener(topics = KafkaTopics.INSTANCE_CONFIG_EVENTS_V1, groupId = "${app.kafka.config-broadcast-group}")
    public void onInstanceConfigurationChanged(InstanceConfigurationEvent event) {
        if (event.getScope() != InstanceConfigurationScopeNames.AUTH) {
            log.debug("Ignoring instance config event for non-auth scope. scope={}, eventId={}", event.getScope(),
                    event.getEventId());
            return;
        }

        Set<InstanceConfigurationKey> changedKeys = event.getChangedKeys();
        boolean evictRedis = changedKeys == null || changedKeys.isEmpty() || intersects(changedKeys, REDIS_AUTH_KEYS);
        boolean evictCaffeine = changedKeys == null || changedKeys.isEmpty()
                || intersects(changedKeys, CAFFEINE_AUTH_SECRET_KEYS);

        if (evictRedis) {
            featureFlagService.evict();
        }

        if (evictCaffeine) {
            userAuthRuntimeSecretService.evict();
        }

        log.info(
                "Handled instance config AUTH event. redisEvicted={}, caffeineEvicted={}, changedKeys={}, globalVersion={}, eventId={}",
                evictRedis, evictCaffeine, changedKeys, event.getGlobalVersion(), event.getEventId());
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
