package com.syncturtle.services.workspace.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.common.core.exceptions.SlugAlreadyExistsException;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.response.WorkspaceMemberInvitationResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;
import com.syncturtle.services.workspace.messaging.kafka.factory.WorkspaceEventFactory;
import com.syncturtle.services.workspace.messaging.outbox.WorkspaceOutboxWriter;
import com.syncturtle.services.workspace.model.UserLite;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.WorkspaceMember;
import com.syncturtle.services.workspace.model.param.WorkspaceCreateParam;
import com.syncturtle.services.workspace.model.param.WorkspaceMemberCreateParam;
import com.syncturtle.services.workspace.repository.UserRepository;
import com.syncturtle.services.workspace.repository.WorkspaceMemberInviteRepository;
import com.syncturtle.services.workspace.repository.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.repository.WorkspaceRepository;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceProjection;
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

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMemberInviteRepository workspaceMemberInviteRepository;
    private final UserRepository userRepository;
    private final WorkspaceEventFactory workspaceEventFactory;
    private final WorkspaceOutboxWriter outboxWriter;
    private final WorkspaceSlugPolicy workspaceSlugPolicy;
    private final WorkspaceApiMapper mapper;

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
            throw new SlugAlreadyExistsException("That URL is taken.", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getCurrentUserWorkspaces(UUID currentUserId) {
        Assert.notNull(currentUserId, "currentUserId is required");

        return workspaceMemberRepository.findCurrentUserWorkspaces(currentUserId)
                .stream()
                .map(mapper::toWorkspaceResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceMemberInvitationResponse> listCurrentUserInvitations(UUID currentUserId) {
        Assert.notNull(currentUserId, "currentUserId is required");

        UserLite user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user was not found."));

        String email = normalizeEmail(user.getEmail());

        return workspaceMemberInviteRepository.findCurrentUserInvitationsByEmail(email)
                .stream()
                .map(mapper::toWorkspaceMemberInvitationResponse)
                .toList();
    }

    private static String normalizeEmail(String email) {
        Assert.hasText(email, "email is required");

        return email.trim().toLowerCase(Locale.ROOT);
    }

}
