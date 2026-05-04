package com.syncturtle.services.workspace.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.syncturtle.services.workspace.models.Workspace;
import com.syncturtle.services.workspace.repositories.projections.WorkspaceProjection;
import com.syncturtle.services.workspace.repositories.query.WorkspaceCursor;
import com.syncturtle.services.workspace.repositories.specs.WorkspaceSpecifications;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID>, JpaSpecificationExecutor<Workspace> {
    boolean existsBySlugIgnoreCaseAndDeletedAtIsNull(String slug);

    Optional<WorkspaceProjection> findWorkspaceById(UUID id);

    default List<WorkspaceProjection> findWorkspacePageDesc(String pattern, WorkspaceCursor cursor, int limit) {
        Specification<Workspace> spec = WorkspaceSpecifications.notDeleted()
                .and(WorkspaceSpecifications.nameOrSlugLikeLowercasePattern(pattern));

        if (cursor != null) {
            spec = spec.and(WorkspaceSpecifications.beforeCursorDesc(cursor.createdAt(), cursor.id()));
        }

        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"));

        return this.findBy(spec, q -> q
                .as(WorkspaceProjection.class)
                .sortBy(sort)
                .limit(limit)
                .all());
    }
}
