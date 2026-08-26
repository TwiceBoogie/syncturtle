package com.syncturtle.services.workspace.repository.spec;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.syncturtle.services.workspace.model.Workspace;

public final class WorkspaceSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private WorkspaceSpecifications() {
    }

    public static Specification<Workspace> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /**
     * pattern should already include %...% and be lowercase. If pattern is null,
     * no-op.
     * 
     * @param pattern
     * @return
     */
    public static Specification<Workspace> nameOrSlugLikeLowercasePattern(String pattern) {
        if (!StringUtils.hasText(pattern)) {
            return (root, query, cb) -> cb.conjunction();
        }

        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.get("slug")), pattern, LIKE_ESCAPE));
    }

    /**
     * Seek predicates for DESC ordering:
     * (createdAt, id) < (cursorCreatedAt, cursorId)
     * 
     * @param cursorCreatedAt
     * @param cursorId
     * @return
     */
    public static Specification<Workspace> beforeCursorDesc(Instant cursorCreatedAt, UUID cursorId) {
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
