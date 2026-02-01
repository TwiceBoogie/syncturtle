package com.syncturtle.platform.services.user.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.platform.services.user.models.User;
import com.syncturtle.platform.services.user.repositories.projections.UserPasswordAutosetProjection;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailIgnoreCase(String email);

    Optional<UserPasswordAutosetProjection> findByEmailIgnoreCase(String email);
}
