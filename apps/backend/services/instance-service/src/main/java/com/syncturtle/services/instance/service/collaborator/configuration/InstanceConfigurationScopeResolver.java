package com.syncturtle.services.instance.service.collaborator.configuration;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

public final class InstanceConfigurationScopeResolver {

    private InstanceConfigurationScopeResolver() {
    }

    public static EnumSet<InstanceConfigurationScope> scopesOf(Set<InstanceConfigurationKey> keys) {
        EnumSet<InstanceConfigurationScope> scopes = EnumSet.noneOf(InstanceConfigurationScope.class);
        for (InstanceConfigurationKey key : keys) {
            scopes.addAll(key.scopes());
        }
        return scopes;
    }

    public static Set<InstanceConfigurationKey> keysInScope(Set<InstanceConfigurationKey> keys,
            InstanceConfigurationScope scope) {
        return keys.stream()
                .filter(key -> key.belongsTo(scope))
                .collect(Collectors.toUnmodifiableSet());
    }

}
