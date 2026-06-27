package com.syncturtle.services.workspace.service.impl;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.contracts.workspace.error.WorkspaceErrorCode;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.common.contracts.workspace.exception.WorkspaceException;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;
import com.syncturtle.services.workspace.messaging.kafka.factory.WorkspaceEventFactory;
import com.syncturtle.services.workspace.messaging.outbox.WorkspaceOutboxWriter;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.WorkspaceMember;
import com.syncturtle.services.workspace.model.param.WorkspaceCreateParam;
import com.syncturtle.services.workspace.model.param.WorkspaceMemberCreateParam;
import com.syncturtle.services.workspace.repository.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.repository.WorkspaceRepository;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceProjection;
import com.syncturtle.services.workspace.service.WorkspaceAdminService;
import com.syncturtle.services.workspace.service.mapper.WorkspaceApiMapper;
import com.syncturtle.services.workspace.service.workspace.WorkspaceSlugPolicy;
import com.syncturtle.services.workspace.type.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceAdminServiceImpl implements WorkspaceAdminService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceSlugPolicy workspaceSlugPolicy;
    private final WorkspaceOutboxWriter outboxWriter;
    private final WorkspaceEventFactory workspaceEventFactory;
    private final WorkspaceApiMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public WorkspaceSlugCheckResponse checkSlug(UUID currentUserId, String slug) {
        Assert.notNull(currentUserId, "currentUserId is required");

        if (!StringUtils.hasText(slug)) {
            return WorkspaceSlugCheckResponse.unavailable("Workspace slug is required");
        }

        try {
            String normalizedSlug = workspaceSlugPolicy.normalize(slug);

            workspaceSlugPolicy.assertValidFormat(normalizedSlug);
            workspaceSlugPolicy.assertAvailable(normalizedSlug);

            return WorkspaceSlugCheckResponse.available();
        } catch (WorkspaceException exception) {
            return WorkspaceSlugCheckResponse.unavailable(exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return WorkspaceSlugCheckResponse.unavailable(exception.getMessage());
        }
    }

    @Override
    @Transactional
    public WorkspaceResponse createWorkspace(UUID currentUserId, WorkspaceCreateRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(request, "workspace create request is required");

        String normalizedSlug = workspaceSlugPolicy.normalizeAndAssertAvailable(request.getSlug());

        WorkspaceCreateParam workspaceParam = WorkspaceCreateParam.builder()
                .name(request.getName())
                .slug(normalizedSlug)
                .organizationSize(request.getOrganizationSize())
                .ownerId(currentUserId)
                .build();

        try {
            Workspace workspace = workspaceRepository.saveAndFlush(Workspace.create(workspaceParam));

            WorkspaceMemberCreateParam memberParam = WorkspaceMemberCreateParam.builder()
                    .role(WorkspaceRole.ADMIN)
                    .memberId(currentUserId)
                    .workspace(workspace)
                    .companyRole(request.getCompanyRole())
                    .build();

            WorkspaceMember member = workspaceMemberRepository.saveAndFlush(WorkspaceMember.create(memberParam));

            CurrentUserWorkspaceProjection projection = workspaceMemberRepository
                    .findCurrentUserWorkspaceById(currentUserId, workspace.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Workspace was created but could not be loaded."));

            WorkspaceEvent workspaceEvent = workspaceEventFactory.created(workspace);
            WorkspaceMemberEvent workspaceMemberEvent = workspaceEventFactory.memberCreated(member, workspace.getId());

            outboxWriter.saveWorkspaceEvent(workspaceEvent);
            outboxWriter.saveWorkspaceMemberEvent(workspaceMemberEvent);

            return mapper.toWorkspaceResponse(projection);
        } catch (DataIntegrityViolationException exception) {
            throw WorkspaceException.of(WorkspaceErrorCode.WORKSPACE_CREATE_FAILED);
        }
    }

}
