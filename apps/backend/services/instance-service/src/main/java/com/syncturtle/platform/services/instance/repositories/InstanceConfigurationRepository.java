package com.syncturtle.platform.services.instance.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.models.InstanceConfiguration;

public interface InstanceConfigurationRepository extends JpaRepository<InstanceConfiguration, UUID> {
    Optional<InstanceConfiguration> findByKey(InstanceConfigurationKey key);

    boolean existsByKey(InstanceConfigurationKey key);

    List<InstanceConfiguration> findByKeyIn(Collection<InstanceConfigurationKey> keys);
}
