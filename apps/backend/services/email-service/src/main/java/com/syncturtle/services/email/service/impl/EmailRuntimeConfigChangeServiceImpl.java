package com.syncturtle.services.email.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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

    private final EmailRuntimeConfigResolver runtimeConfigResolver;

    @Override
    public void receive(InstanceConfigurationEvent event) {
        Assert.notNull(event, "instance configuration event is required");

        if (!InstanceConfigurationScope.EMAIL.equals(event.getScope())) {
            log.debug("Ignoring instance configuration event for non-email scope. scope={}, eventId={}",
                    event.getScope(), event.getEventId());

            return;
        }

        Assert.isTrue(event.getGlobalVersion() >= 0, "globalVersion must not be negative");
        Assert.isTrue(event.getScopeVersion() >= 0, "scopeVersion must not be negative");

        runtimeConfigResolver.refreshIfOlderThan(event.getGlobalVersion());

        log.info("Email runtime configuration refreshed. scope={}, scopeVersion={}, globalVersion={}, eventId={}",
                event.getScope(), event.getScopeVersion(), event.getGlobalVersion(), event.getEventId());
    }

}
