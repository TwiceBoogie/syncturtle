package com.syncturtle.platform.services.workspace.services.impl;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.core.exceptions.SlugAlreadyExistsException;
import com.syncturtle.common.core.utils.WorkspaceSlugRules;
import com.syncturtle.common.spring.mapping.CursorCodec;
import com.syncturtle.common.spring.mapping.CursorCodec.DecodedCursor;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.platform.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.platform.services.workspace.dto.response.WorkspaceSlugCheckResponse;
import com.syncturtle.platform.services.workspace.enums.WorkspaceRole;
import com.syncturtle.platform.services.workspace.models.Workspace;
import com.syncturtle.platform.services.workspace.models.WorkspaceMember;
import com.syncturtle.platform.services.workspace.repositories.WorkspaceMemberRepository;
import com.syncturtle.platform.services.workspace.repositories.WorkspaceRepository;
import com.syncturtle.platform.services.workspace.repositories.projections.WorkspaceProjection;
import com.syncturtle.platform.services.workspace.repositories.query.WorkspaceCursor;
import com.syncturtle.platform.services.workspace.services.WorkspaceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    // repositories
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final RequestUserContext userContext;
    // helpers
    private final CursorCodec cursorCodec;

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceProjection> workspaceAll(String cursor, int perPage, String search) {
        // 1: decode cursor which contains (ID, createdAt)
        DecodedCursor decoded = cursorCodec.decode(cursor);

        // 2: build pattern to avoid db side concatenation
        String pattern = (search == null || search.isBlank()) ? null : "%" + search.trim().toLowerCase() + "%";

        WorkspaceCursor workspaceCursor = (decoded == null) ? null
                : new WorkspaceCursor(decoded.createdAt(), decoded.id());
        return workspaceRepository.findWorkspacePageDesc(pattern, workspaceCursor, perPage + 1);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceSlugCheckResponse workspaceSlugCheck(String slug) {
        if (slug == null || slug.isBlank()) {
            return new WorkspaceSlugCheckResponse(false, "Workspace slug is required");
        }

        String normalized = normalizeSlug(slug);

        try {
            validateSlugOrThrow(normalized);
            return new WorkspaceSlugCheckResponse(true, null);
        } catch (SlugAlreadyExistsException exception) {
            return new WorkspaceSlugCheckResponse(false, exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return new WorkspaceSlugCheckResponse(false, exception.getMessage());
        }
    }

    @Override
    @Transactional
    public WorkspaceProjection workspaceCreate(WorkspaceCreateRequest request) {
        String name = request.getName().trim();
        String slug = normalizeSlug(request.getSlug());
        String organizationSize = request.getOrganizationSize().trim();
        String companyRole = (request.getCompanyRole() == null) ? "" : request.getCompanyRole().trim();

        // revalidate on write
        validateSlugOrThrow(slug);

        try {
            Workspace workspace = workspaceRepository
                    .save(Workspace.create(name, slug, organizationSize, userContext.getUserId()));

            workspaceMemberRepository
                    .save(WorkspaceMember.create(WorkspaceRole.ADMIN, userContext.getUserId(), workspace, companyRole));

            return workspaceRepository.findWorkspaceById(workspace.getId())
                    .orElseThrow(() -> new IllegalStateException("Workspace created but could not be loaded."));
        } catch (DataIntegrityViolationException exception) {
            throw new SlugAlreadyExistsException("That URL is taken.", exception);
        }
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase();
    }

    private void validateSlugOrThrow(String normalizedSlug) {
        if (normalizedSlug.length() < 3) {
            throw new IllegalArgumentException("Slug must be at least 3 characters.");
        }
        if (normalizedSlug.startsWith("-") || normalizedSlug.endsWith("-")) {
            throw new IllegalArgumentException("Slug can't start or end with a hyphen.");
        }
        if (WorkspaceSlugRules.RESTRICTED.contains(normalizedSlug)) {
            throw new IllegalArgumentException("That URL is reserved.");
        }

        boolean exists = workspaceRepository.existsBySlugIgnoreCaseAndDeletedAtIsNull(normalizedSlug);
        if (exists) {
            throw new SlugAlreadyExistsException("That URL is taken.");
        }
    }

}
