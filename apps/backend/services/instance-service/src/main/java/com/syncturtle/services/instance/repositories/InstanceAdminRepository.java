package com.syncturtle.services.instance.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.instance.models.InstanceAdmin;
import com.syncturtle.services.instance.repositories.projections.InstanceAdminProjection;

public interface InstanceAdminRepository extends JpaRepository<InstanceAdmin, UUID> {
    boolean existsByUserIdAndRoleGreaterThanEqual(UUID userId, int role);

    List<InstanceAdminProjection> findAllByInstance_Id(UUID instanceId);

    boolean existsByIdIsNotNull();

    boolean existsByInstance_IdAndUserId(UUID instanceId, UUID userId);

    boolean existsByUserId(UUID userId);

    // return number of rows deleted
    long deleteByInstance_IdAndId(UUID instanceId, UUID id);

    Optional<InstanceAdmin> findByInstance_IdAndUserId(UUID instanceId, UUID userId);

    List<InstanceAdmin> findByInstance_Id(UUID instanceId);
}
