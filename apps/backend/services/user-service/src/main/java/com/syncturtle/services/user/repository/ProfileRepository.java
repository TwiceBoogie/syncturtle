package com.syncturtle.services.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncturtle.services.user.model.Profile;
import com.syncturtle.services.user.repository.projection.UserSettingsProfileProjection;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
    <T> Optional<T> findByUser_Id(UUID userId, Class<T> clazz);

    Optional<UserSettingsProfileProjection> findSettingsByUser_Id(UUID userId);

    boolean existsByUserId(UUID userId);
}
