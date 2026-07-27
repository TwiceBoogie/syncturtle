package com.syncturtle.services.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.projection.UserSettingsIdentityProjection;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailIgnoreCase(String email);

    <T> Optional<T> findByEmailIgnoreCase(String email, Class<T> clazz);

    Optional<UserSettingsIdentityProjection> findSettingsIdentityById(UUID id);

    Optional<User> findByIdAndDeletedAtIsNull(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT user
            FROM User user
            WHERE LOWER(user.email) = LOWER(:email)
            """)
    Optional<User> findByEmailIgnoreCaseForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT user
            FROM User user
            WHERE user.id = :userId
            """)
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);
}
