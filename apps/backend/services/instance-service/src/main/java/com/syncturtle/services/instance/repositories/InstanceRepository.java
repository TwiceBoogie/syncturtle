package com.syncturtle.services.instance.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.instance.models.Instance;

public interface InstanceRepository extends JpaRepository<Instance, UUID> {

    Optional<Instance> findFirstByDeletedAtIsNullOrderByCreatedAtDesc();

    // very first entity
    Optional<Instance> findFirstByOrderByCreatedAtAsc();

    // latest entity
    Optional<Instance> findFirstByOrderByCreatedAtDesc();

    <T> Optional<T> findTopByOrderByCreatedAtDesc(Class<T> clazz);

    boolean existsByIdIsNotNull();
}
