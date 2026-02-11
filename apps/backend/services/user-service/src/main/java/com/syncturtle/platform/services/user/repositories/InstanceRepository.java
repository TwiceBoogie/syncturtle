package com.syncturtle.platform.services.user.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.platform.services.user.models.Instance;

public interface InstanceRepository extends JpaRepository<Instance, UUID> {
    Optional<Instance> findFirstByOrderByCreatedAtAsc();
}
