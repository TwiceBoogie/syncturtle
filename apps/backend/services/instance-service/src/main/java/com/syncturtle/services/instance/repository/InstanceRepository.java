package com.syncturtle.services.instance.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.instance.model.Instance;

public interface InstanceRepository extends JpaRepository<Instance, UUID> {

    <T> Optional<T> findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Class<T> clazz);

    /**
     * Find oldest entity
     * 
     * @return oldest Instance entity
     */
    Optional<Instance> findFirstByOrderByCreatedAtAsc();

    <T> Optional<T> findTopByOrderByCreatedAtDesc(Class<T> clazz);

    boolean existsByIdIsNotNull();
}
