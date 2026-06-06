package com.syncturtle.services.instance.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncturtle.services.instance.model.User;
import com.syncturtle.services.instance.repository.projection.AdminUserDetailLiteProjection;

public interface UserRepository extends JpaRepository<User, UUID> {
    @Query("""
            SELECT u.id
            FROM User u
            WHERE lower(u.email) = lower(:email)
            """)
    Optional<UUID> findIdByEmailIgnoreCase(@Param("email") String email);

    long countByActiveTrue();

    Optional<User> findFirstByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AdminUserDetailLiteProjection> findByIdInAndDeletedAtIsNull(Collection<UUID> ids);

    AdminUserDetailLiteProjection findByIdAndDeletedAtIsNull(UUID id);
}
