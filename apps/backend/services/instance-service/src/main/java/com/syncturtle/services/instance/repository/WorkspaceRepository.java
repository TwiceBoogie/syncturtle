package com.syncturtle.services.instance.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syncturtle.services.instance.model.WorkspaceLite;
import com.syncturtle.services.instance.repository.projection.InstanceWorkspaceProjection;
import com.syncturtle.services.instance.repository.query.InstanceWorkspaceSql;

public interface WorkspaceRepository extends JpaRepository<WorkspaceLite, UUID> {
    Optional<WorkspaceLite> findById(UUID id);

    long countByDeletedAtIsNull();

    @Query(value = InstanceWorkspaceSql.FIND_WORKSPACE_PAGE_DESC, nativeQuery = true)
    List<InstanceWorkspaceProjection> findWorkspacePageDesc(@Param("pattern") String pattern,
            @Param("cursorCreatedAt") Instant cursorCreatedAt, @Param("cursorId") UUID cursorId,
            @Param("limit") int limit);
}
