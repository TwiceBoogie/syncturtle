package com.syncturtle.platform.services.instance.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.platform.services.instance.models.InstanceAdmin;

public interface InstanceAdminRepository extends JpaRepository<InstanceAdmin, UUID> {
    boolean existsByUserIdAndRoleGreaterThanEqual(UUID userId, int role);
}
