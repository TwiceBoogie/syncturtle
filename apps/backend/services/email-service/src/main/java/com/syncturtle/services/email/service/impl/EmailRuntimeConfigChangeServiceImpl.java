package com.syncturtle.services.email.service.impl;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.services.email.service.EmailRuntimeConfigChangeService;
import com.syncturtle.services.email.service.collaborator.EmailRuntimeConfigResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailRuntimeConfigChangeServiceImpl implements EmailRuntimeConfigChangeService {

    private static final Set<InstanceConfigurationKey> EMAIL_CACHE_KEYS = EnumSet.of(
            InstanceConfigurationKey.ENABLE_SMTP,
            InstanceConfigurationKey.EMAIL_HOST,
            InstanceConfigurationKey.EMAIL_HOST_USER,
            InstanceConfigurationKey.EMAIL_HOST_PASSWORD,
            InstanceConfigurationKey.EMAIL_PORT,
            InstanceConfigurationKey.EMAIL_FROM,
            InstanceConfigurationKey.EMAIL_USE_TLS,
            InstanceConfigurationKey.EMAIL_USE_SSL);

    private final EmailRuntimeConfigResolver runtimeConfigResolver;

    @Override
    public void receive(InstanceConfigurationEvent event) {
        Assert.notNull(event, "instance configuration event is required");

        if (event.getScope() != InstanceConfigurationScope.EMAIL) {
            return;
        }
        if (!event.affectsAny(EMAIL_CACHE_KEYS)) {
            return;
        }

        runtimeConfigResolver.refreshIfOlderThan(event.getConfigurationVersion());
        log.info("Email runtime configuration refreshed. scope={} configurationVersion={} eventId={}",
                event.getScope(), event.getConfigurationVersion(), event.getEventId());
    }

}
