package com.syncturtle.platform.services.instance.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.platform.services.instance.models.InstanceAdmin;
import com.syncturtle.platform.services.instance.repositories.projections.InstanceAdminProjection;

public interface InstanceAdminRepository extends JpaRepository<InstanceAdmin, UUID> {
    boolean existsByUserIdAndRoleGreaterThanEqual(UUID userId, int role);

    List<InstanceAdminProjection> findAllByInstance_Id(UUID instanceId);
}
