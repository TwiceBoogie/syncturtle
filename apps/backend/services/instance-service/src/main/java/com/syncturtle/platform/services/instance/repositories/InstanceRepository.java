package com.syncturtle.platform.services.instance.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.syncturtle.platform.services.instance.models.Instance;
import com.syncturtle.platform.services.instance.models.readmodel.InstanceInfoRow;

public interface InstanceRepository extends JpaRepository<Instance, UUID> {

    @Query("""
            SELECT new com.syncturtle.platform.services.instance.models.readmodel.InstanceInfoRow(
                i.id,
                i.instanceName,
                i.whitelistEmails,
                i.instanceId,
                i.updateCheck.currentVersion,
                i.updateCheck.latestVersion,
                i.updateCheck.lastCheckedAt,
                i.runtime.namespace,
                i.telemetryEnabled,
                i.supportRequired,
                i.setupDone,
                i.signupScreenVisited,
                i.verified,
                i.createdAt,
                i.updatedAt,
                i.createdById,
                i.updatedById
            )
            FROM Instance i
            WHERE i.deletedAt is NULL
            ORDER BY i.createdAt desc
            """)
    Optional<InstanceInfoRow> findLatestInfoRow();

    Optional<Instance> findFirstByDeletedAtIsNullOrderByCreatedAtAsc();

    <T> Optional<T> findTopByOrderByCreatedAtDesc(Class<T> clazz);
}
