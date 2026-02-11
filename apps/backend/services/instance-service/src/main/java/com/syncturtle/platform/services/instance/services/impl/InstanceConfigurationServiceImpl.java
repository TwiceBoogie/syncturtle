package com.syncturtle.platform.services.instance.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.platform.services.instance.models.InstanceConfiguration;
import com.syncturtle.platform.services.instance.repositories.InstanceConfigurationRepository;
import com.syncturtle.platform.services.instance.services.InstanceConfigurationService;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationCrypto;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceConfigurationServiceImpl implements InstanceConfigurationService {

    // repositories
    private final InstanceConfigurationRepository iConfigurationRepository;
    private final InstanceConfigurationResolver instanceConfigurationResolver;
    private final InstanceConfigurationCrypto crypto;

    @Override
    public Map<InstanceConfigurationKey, String> configurations() {
        return instanceConfigurationResolver.resolveRequested(List.of(
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.EMAIL_HOST),
                InstanceConfigurationResolver.RequestedKey.of(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstanceConfigurationResponse> configurationsAll() {
        List<InstanceConfiguration> all = iConfigurationRepository.findAll();
        List<InstanceConfigurationResponse> result = new ArrayList<>();
        for (InstanceConfiguration instanceConfiguration : all) {
            result.add(InstanceConfigurationResponse.builder()
                    .id(instanceConfiguration.getId())
                    .key(instanceConfiguration.getKey()).value(crypto.decryptIfNeeded(instanceConfiguration))
                    .createdAt(instanceConfiguration.getCreatedAt()).updatedAt(instanceConfiguration.getUpdatedAt())
                    .createdById(instanceConfiguration.getCreatedById())
                    .updatedById(instanceConfiguration.getUpdatedById()).build());
        }
        return result;
    }

    @Override
    @Transactional
    public List<InstanceConfigurationResponse> configurationsUpdate(Map<InstanceConfigurationKey, String> request) {
        List<InstanceConfiguration> configurations = iConfigurationRepository.findByKeyIn(request.keySet());

        for (InstanceConfiguration configuration : configurations) {
            String incoming = request.getOrDefault(configuration.getKey(), configuration.getValue());

            configuration.setValue(crypto.encryptIfNeeded(configuration.isEncrypted(), incoming));
        }

        configurations = iConfigurationRepository.saveAll(configurations);

        List<InstanceConfigurationResponse> result = new ArrayList<>();
        for (InstanceConfiguration configuration : configurations) {
            result.add(InstanceConfigurationResponse.builder()
                    .id(configuration.getId())
                    .key(configuration.getKey())
                    .value(crypto.decryptIfNeeded(configuration))
                    .createdAt(configuration.getCreatedAt())
                    .updatedAt(configuration.getUpdatedAt())
                    .createdById(configuration.getCreatedById())
                    .updatedById(configuration.getUpdatedById())
                    .build());
        }
        return result;
    }

}
