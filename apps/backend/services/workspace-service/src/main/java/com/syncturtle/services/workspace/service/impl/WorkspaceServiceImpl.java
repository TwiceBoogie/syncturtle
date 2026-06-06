package com.syncturtle.services.workspace.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.core.exceptions.SlugAlreadyExistsException;
import com.syncturtle.common.web.pagination.CursorCodec;
import com.syncturtle.common.web.pagination.CursorPageResponse;
import com.syncturtle.common.web.pagination.CursorPosition;
import com.syncturtle.common.web.pagination.DecodedCursor;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.WorkspaceMember;
import com.syncturtle.services.workspace.model.param.WorkspaceCreateParam;
import com.syncturtle.services.workspace.model.param.WorkspaceMemberCreateParam;
import com.syncturtle.services.workspace.repository.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.repository.WorkspaceRepository;
import com.syncturtle.services.workspace.repository.projection.WorkspaceProjection;
import com.syncturtle.services.workspace.service.WorkspaceService;
import com.syncturtle.services.workspace.service.mapper.WorkspaceApiMapper;
import com.syncturtle.services.workspace.service.workspace.WorkspaceSlugPolicy;
import com.syncturtle.services.workspace.type.WorkspaceRole;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private static final int MAX_PER_PAGE = 100;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CursorCodec cursorCodec;
    private final WorkspaceSlugPolicy workspaceSlugPolicy;
    private final WorkspaceApiMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<WorkspaceResponse> workspaceAll(
            UUID currentUserId,
            String cursor,
            int perPage,
            String search) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.isTrue(perPage > 0, "perPage must be greater than 0");
        Assert.isTrue(perPage <= MAX_PER_PAGE, "perPage must be less than or equal to " + MAX_PER_PAGE);

        // 1: decode cursor which contains (ID, createdAt)
        DecodedCursor decoded = cursorCodec.decode(cursor);

        CursorPosition cursorPosition = decoded == null
                ? null
                : decoded.toPosition();

        // 2: build pattern to avoid db side concatenation
        String pattern = toSearchPattern(search);

        /**
         * Important:
         * 
         * If workspaces are user-scoped, the repository should eventually filter by
         * currentUserId through workspace_members.
         */
        List<WorkspaceProjection> workspaces = workspaceRepository.findWorkspacePageDesc(pattern, cursorPosition,
                perPage + 1);
        return mapper.toCursorPageWorkspaceResponse(workspaces, perPage);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceSlugCheckResponse workspaceSlugCheck(String slug) {
        if (!StringUtils.hasText(slug)) {
            return WorkspaceSlugCheckResponse.unavailable("Workspace slug is required");
        }

        try {
            String normalizedSlug = workspaceSlugPolicy.normalize(slug);

            workspaceSlugPolicy.assertValidFormat(normalizedSlug);
            workspaceSlugPolicy.assertAvailable(normalizedSlug);

            return WorkspaceSlugCheckResponse.available();
        } catch (SlugAlreadyExistsException exception) {
            return WorkspaceSlugCheckResponse.unavailable(exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return WorkspaceSlugCheckResponse.unavailable(exception.getMessage());
        }
    }

    @Override
    @Transactional
    public WorkspaceResponse workspaceCreate(UUID currentUserId, WorkspaceCreateRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(request, "workspace create request is required");

        String normalizedSlug = workspaceSlugPolicy.normalizeAndAssertAvailable(request.getSlug());

        WorkspaceCreateParam WorkspaceParam = WorkspaceCreateParam.builder()
                .name(request.getName())
                .slug(normalizedSlug)
                .organizationSize(request.getOrganizationSize())
                .ownerId(currentUserId)
                .build();

        try {
            Workspace workspace = workspaceRepository.save(Workspace.create(WorkspaceParam));

            WorkspaceMemberCreateParam memberParam = WorkspaceMemberCreateParam.builder()
                    .role(WorkspaceRole.MEMBER)
                    .memberId(currentUserId)
                    .workspace(workspace)
                    .companyRole(request.getCompanyRole())
                    .build();

            workspaceMemberRepository.save(WorkspaceMember.create(memberParam));

            WorkspaceProjection projection = workspaceRepository.findWorkspaceById(workspace.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Workspace was created but could not be loaded."));

            return mapper.toWorkspaceResponse(projection);
        } catch (DataIntegrityViolationException exception) {
            throw new SlugAlreadyExistsException("That URL is taken.", exception);
        }
    }

    private static String toSearchPattern(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }

        String normalized = search.trim().toLowerCase();

        return "%" + normalized + "%";
    }

}
