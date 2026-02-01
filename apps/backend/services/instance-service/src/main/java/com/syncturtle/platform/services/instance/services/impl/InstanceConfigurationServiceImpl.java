package com.syncturtle.platform.services.instance.services.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.services.InstanceConfigurationService;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceConfigurationServiceImpl implements InstanceConfigurationService {

    private final InstanceConfigurationResolver instanceConfigurationResolver;

    @Override
    public Map<InstanceConfigurationKey, String> configurations() {
        return instanceConfigurationResolver.resolveRequested(List.of(
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN)));
    }

}
