package com.syncturtle.services.workspace.service.runtime;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.syncturtle.common.contracts.workspace.config.WorkspaceRuntimeConfigResponse;
import com.syncturtle.services.workspace.client.InstanceClient;
import com.syncturtle.services.workspace.configuration.cache.WorkspaceFlagsRuntimeCacheNames;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceConfigResolver {

    private final InstanceClient instanceClient;

    @Cacheable(cacheNames = WorkspaceFlagsRuntimeCacheNames.WORKSPACE_FLAGS_RUNTIME, key = WorkspaceFlagsRuntimeCacheNames.CURRENT_SPEL_KEY, unless = "#result == null", sync = true)
    public WorkspaceFlagRuntimeSnapshot getInstanceConfigurations() {
        WorkspaceRuntimeConfigResponse response = instanceClient.getWorkspaceRuntimeConfigResponse();

        return WorkspaceFlagRuntimeSnapshot.builder()
                .workspaceCreationDisabled(response.isWorkspaceCreationDisabled())
                .version(response.getVersion())
                .build();
    }

    @CacheEvict(cacheNames = WorkspaceFlagsRuntimeCacheNames.WORKSPACE_FLAGS_RUNTIME, key = WorkspaceFlagsRuntimeCacheNames.CURRENT_SPEL_KEY)
    public void evict() {
        // annotation does the eviction for us
    }

}
