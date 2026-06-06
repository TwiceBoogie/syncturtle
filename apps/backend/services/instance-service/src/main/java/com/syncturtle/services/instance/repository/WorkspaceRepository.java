package com.syncturtle.services.instance.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.syncturtle.common.web.pagination.CursorPosition;
import com.syncturtle.services.instance.model.Workspace;
import com.syncturtle.services.instance.repository.projection.InstanceWorkspaceProjection;
import com.syncturtle.services.instance.repository.spec.WorkspaceSpecifications;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID>, JpaSpecificationExecutor<Workspace> {
    Optional<Workspace> findById(UUID id);

    long countByDeletedAtIsNull();

    default List<InstanceWorkspaceProjection> findWorkspacePageDesc(
            String pattern,
            CursorPosition cursor,
            int limit) {
        Specification<Workspace> spec = WorkspaceSpecifications.active()
                .and(WorkspaceSpecifications.nameOrSlugLikeLowercasePattern(pattern));

        if (cursor != null) {
            spec = spec.and(
                    WorkspaceSpecifications.beforeCursorDesc(cursor.getCreatedAt(),
                            cursor.getId()));
        }

        Sort sort = Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"));

        return this.findBy(spec, query -> query
                .as(InstanceWorkspaceProjection.class)
                .sortBy(sort)
                .limit(limit)
                .all());
    }
}
