package com.syncturtle.services.instance.repository.spec;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.syncturtle.services.instance.model.WorkspaceLite;

public final class WorkspaceSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private WorkspaceSpecifications() {
    }

    public static Specification<WorkspaceLite> active() {
        return (root, query, cb) -> cb.and(
                cb.isNull(root.get("deletedAt")));
    }

    public static Specification<WorkspaceLite> nameOrSlugLikeLowercasePattern(String pattern) {
        if (!StringUtils.hasText(pattern)) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("slug")), pattern, LIKE_ESCAPE));
    }

    public static Specification<WorkspaceLite> beforeCursorDesc(Instant cursorCreatedAt, UUID cursorId) {
        if (cursorCreatedAt == null || cursorId == null) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> cb.or(
                cb.lessThan(root.get("createdAt"), cursorCreatedAt),
                cb.and(
                        cb.equal(root.get("createdAt"), cursorCreatedAt),
                        cb.lessThan(root.get("id"), cursorId)));
    }

}
