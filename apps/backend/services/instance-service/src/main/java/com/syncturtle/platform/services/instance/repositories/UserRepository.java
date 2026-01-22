package com.syncturtle.platform.services.instance.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.platform.services.instance.models.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findFirstByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
