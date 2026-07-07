package com.syncturtle.services.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.projection.UserSettingsIdentityProjection;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailIgnoreCase(String email);

    <T> Optional<T> findByEmailIgnoreCase(String email, Class<T> clazz);

    Optional<UserSettingsIdentityProjection> findSettingsIdentityById(UUID id);

    Optional<User> findByIdAndDeletedAtIsNull(UUID userId);
}
