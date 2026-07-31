package com.syncturtle.services.workspace.service.collaborator.workspace;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.error.WorkspaceErrorCode;
import com.syncturtle.common.contracts.workspace.exception.WorkspaceException;
import com.syncturtle.common.contracts.workspace.validation.WorkspaceSlugs;
import com.syncturtle.services.workspace.repository.WorkspaceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class WorkspaceSlugPolicy {

    private static final int MIN_SLUG_LENGTH = 3;

    private final WorkspaceRepository workspaceRepository;

    public String normalize(String slug) {
        Assert.hasText(slug, "slug is required");

        return slug.trim().toLowerCase();
    }

    public void assertValidFormat(String normalizeSlug) {
        Assert.hasText(normalizeSlug, "normalizedSlug is required");

        if (normalizeSlug.length() < MIN_SLUG_LENGTH) {
            throw new IllegalArgumentException("Slug must be at least 3 characters");
        }

        if (normalizeSlug.startsWith("-") || normalizeSlug.endsWith("-")) {
            throw new IllegalArgumentException("Slug can't start or end with hyphen");
        }

        if (WorkspaceSlugs.RESTRICTED.contains(normalizeSlug)) {
            throw new IllegalArgumentException("That URL is reserved");
        }
    }

    public void assertAvailable(String normalizedSlug) {
        Assert.hasText(normalizedSlug, "normalizedSlug is required");

        boolean exists = workspaceRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(normalizedSlug);
        if (exists) {
            throw WorkspaceException.of(WorkspaceErrorCode.WORKSPACE_SLUG_ALREADY_EXISTS);
        }
    }

    public String normalizeAndAssertAvailable(String slug) {
        String normalizedSlug = normalize(slug);

        assertValidFormat(normalizedSlug);
        assertAvailable(normalizedSlug);

        return normalizedSlug;
    }

}
