package com.syncturtle.services.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.user.model.InstanceLite;

public interface InstanceLiteRepository extends JpaRepository<InstanceLite, UUID> {
    Optional<InstanceLite> findFirstByOrderByCreatedAtAsc();
}
