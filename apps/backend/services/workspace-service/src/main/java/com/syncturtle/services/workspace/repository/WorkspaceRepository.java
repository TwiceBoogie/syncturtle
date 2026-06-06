package com.syncturtle.services.workspace.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.syncturtle.common.web.pagination.CursorPosition;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.repository.projection.WorkspaceProjection;
import com.syncturtle.services.workspace.repository.specs.WorkspaceSpecifications;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID>, JpaSpecificationExecutor<Workspace> {
    boolean existsBySlugIgnoreCaseAndDeletedAtIsNull(String slug);

    Optional<WorkspaceProjection> findWorkspaceById(UUID id);

    default List<WorkspaceProjection> findWorkspacePageDesc(String pattern, CursorPosition cursor, int limit) {
        Specification<Workspace> spec = WorkspaceSpecifications.notDeleted()
                .and(WorkspaceSpecifications.nameOrSlugLikeLowercasePattern(pattern));

        if (cursor != null) {
            spec = spec.and(WorkspaceSpecifications.beforeCursorDesc(cursor.getCreatedAt(), cursor.getId()));
        }

        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"));

        return this.findBy(spec, query -> query
                .as(WorkspaceProjection.class)
                .sortBy(sort)
                .limit(limit)
                .all());
    }
}
