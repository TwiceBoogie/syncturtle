package com.syncturtle.services.instance.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncturtle.services.instance.model.InstanceAdmin;
import com.syncturtle.services.instance.repository.projection.AdminUserDetailsProjection;
import com.syncturtle.services.instance.repository.projection.InstanceAdminProjection;
import com.syncturtle.services.instance.type.InstanceAdminRole;

public interface InstanceAdminRepository extends JpaRepository<InstanceAdmin, UUID> {
    List<InstanceAdmin> findAllByInstance_IdAndDeletedAtIsNull(UUID instanceId);

    Optional<InstanceAdmin> findByIdAndDeletedAtIsNull(UUID id);

    Optional<InstanceAdmin> findByInstance_IdAndUserIdAndDeletedAtIsNull(UUID instanceId, UUID userId);

    boolean existsByInstance_IdAndUserIdAndDeletedAtIsNull(UUID instanceId, UUID userId);

    boolean existsByDeletedAtIsNull();

    long countByInstance_IdAndRoleAndDeletedAtIsNull(UUID instanceId, InstanceAdminRole role);

    boolean existsByUserIdAndRoleGreaterThanEqual(UUID userId, InstanceAdminRole role);

    List<InstanceAdmin> findAllByInstance_Id(UUID instanceId);

    boolean existsByIdIsNotNull();

    boolean existsByInstance_IdAndUserId(UUID instanceId, UUID userId);

    boolean existsByUserId(UUID userId);

    // return number of rows deleted
    long deleteByInstance_IdAndId(UUID instanceId, UUID id);

    Optional<InstanceAdmin> findByInstance_IdAndUserId(UUID instanceId, UUID userId);

    List<InstanceAdmin> findByInstance_Id(UUID instanceId);

    @Query("""
            SELECT admin.role
            FROM InstanceAdmin admin
            WHERE admin.instance.id = :instanceId
                AND admin.userId = :userId
            """)
    Optional<InstanceAdminRole> findActiveRoleByInstanceIdAndUserId(@Param("instanceId") UUID instanceId,
            @Param("userId") UUID userId);

    @Query("""
            SELECT
                ia.userId AS id,
                u.username AS username,
                u.email AS email,
                u.displayName AS displayName,
                u.firstName AS firstName,
                u.lastName AS lastName,
                u.userTimezone AS userTimezone,
                u.avatarAssetId AS avatarAssetId,
                u.coverImageAssetId AS coverImageAssetId,
                u.active AS active,
                u.emailVerified AS emailVerified,
                u.passwordAutoset AS passwordAutoset,
                u.lastLoginMedium AS lastLoginMedium,
                u.principalType AS principalType,
                u.createdAt AS dateJoined
            FROM InstanceAdmin ia
            JOIN UserLite u on u.id = ia.userId
            WHERE ia.instance.id = :instanceId
                AND ia.userId = :userId
                AND ia.deletedAt IS NULL
                AND u.deletedAt IS NULL
            """)
    Optional<AdminUserDetailsProjection> findCurrentAdminDetails(UUID instanceId, UUID userId);

    List<InstanceAdminProjection> findByInstance_IdAndDeletedAtIsNull(UUID instanceId);

}
