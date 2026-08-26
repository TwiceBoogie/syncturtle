package com.syncturtle.services.workspace.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.workspace.error.WorkspaceErrorCode;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.common.contracts.workspace.exception.WorkspaceException;
import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.common.core.exception.SlugAlreadyExistsException;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.request.WorkspaceInvitationRequest;
import com.syncturtle.services.workspace.dto.response.SimpleMessageResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceMemberInvitationResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;
import com.syncturtle.services.workspace.mapper.WorkspaceApiMapper;
import com.syncturtle.services.workspace.messaging.kafka.factory.WorkspaceEventFactory;
import com.syncturtle.services.workspace.messaging.kafka.factory.WorkspaceInvitationEmailEventFactory;
import com.syncturtle.services.workspace.model.UserLite;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.WorkspaceMember;
import com.syncturtle.services.workspace.model.WorkspaceMemberInvite;
import com.syncturtle.services.workspace.model.param.WorkspaceCreateParam;
import com.syncturtle.services.workspace.model.param.WorkspaceMemberCreateParam;
import com.syncturtle.services.workspace.model.param.WorkspaceMemberInviteCreateParam;
import com.syncturtle.services.workspace.repository.UserLiteRepository;
import com.syncturtle.services.workspace.repository.WorkspaceMemberInviteRepository;
import com.syncturtle.services.workspace.repository.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.repository.WorkspaceRepository;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceProjection;
import com.syncturtle.services.workspace.service.WorkspaceService;
import com.syncturtle.services.workspace.service.collaborator.outbox.EmailOutboxWriter;
import com.syncturtle.services.workspace.service.collaborator.outbox.WorkspaceOutboxWriter;
import com.syncturtle.services.workspace.service.collaborator.runtime.WorkspaceConfigResolver;
import com.syncturtle.services.workspace.service.collaborator.runtime.WorkspaceFlagRuntimeSnapshot;
import com.syncturtle.services.workspace.service.collaborator.workspace.WorkspaceSlugPolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private static final int TOKEN_BYTES = 32;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceMemberInviteRepository workspaceMemberInviteRepository;
    private final UserLiteRepository userRepository;
    private final WorkspaceConfigResolver featureFlagService;
    private final WorkspaceEventFactory workspaceEventFactory;
    private final WorkspaceInvitationEmailEventFactory emailEventFactory;
    private final EmailOutboxWriter emailOutboxWriter;
    private final WorkspaceOutboxWriter workspaceOutboxWriter;
    private final WorkspaceSlugPolicy workspaceSlugPolicy;
    private final SecureTokenGenerator tokenGenerator;
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

        WorkspaceFlagRuntimeSnapshot snapshot = featureFlagService.getInstanceConfigurations();

        if (snapshot.isWorkspaceCreationDisabled()) {
            throw WorkspaceException.of(WorkspaceErrorCode.WORKSPACE_CREATE_FAILED);
        }

        String normalizedSlug = workspaceSlugPolicy.normalizeAndAssertAvailable(request.getSlug());

        WorkspaceCreateParam WorkspaceParam = WorkspaceCreateParam.builder()
                .name(request.getName())
                .slug(normalizedSlug)
                .organizationSize(request.getOrganizationSize())
                .ownerId(currentUserId)
                .build();

        try {
            Workspace workspace = workspaceRepository.saveAndFlush(Workspace.create(WorkspaceParam));

            WorkspaceMemberCreateParam memberParam = WorkspaceMemberCreateParam.builder()
                    .role(WorkspaceRole.ADMIN)
                    .memberId(currentUserId)
                    .workspace(workspace)
                    .companyRole(request.getCompanyRole())
                    .build();

            WorkspaceMember member = workspaceMemberRepository.saveAndFlush(WorkspaceMember.create(memberParam));

            CurrentUserWorkspaceProjection projection = workspaceMemberRepository
                    .findCurrentUserWorkspaceById(currentUserId, workspace.getId())
                    .orElseThrow(() -> WorkspaceException.of(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

            WorkspaceEvent workspaceEvent = workspaceEventFactory.created(workspace);
            WorkspaceMemberEvent workspaceMemberEvent = workspaceEventFactory.memberCreated(member, workspace.getId());

            workspaceOutboxWriter.saveWorkspaceEvent(workspaceEvent);
            workspaceOutboxWriter.saveWorkspaceMemberEvent(workspaceMemberEvent);

            return mapper.toWorkspaceResponse(projection);
        } catch (DataIntegrityViolationException exception) {
            throw WorkspaceException.slugAlreadyExists(normalizedSlug);
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

    @Override
    @Transactional
    public SimpleMessageResponse createWorkspaceInvitations(UUID currentUserId, String workspaceSlug,
            WorkspaceInvitationRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.hasText(workspaceSlug, "workspaceSlug is required");
        Assert.notNull(request, "workspace invitation request is required");

        String normalizedSlug = workspaceSlugPolicy.normalize(workspaceSlug);

        Workspace workspace = workspaceRepository.findBySlugAndDeletedAtIsNull(normalizedSlug)
                .orElseThrow(() -> WorkspaceException.notFound(normalizedSlug));

        UserLite currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> WorkspaceException.invitationForbidden(workspace.getId(), currentUserId));

        WorkspaceMember requestingMember = workspaceMemberRepository
                .findByWorkspace_IdAndMemberIdAndActivatedTrue(workspace.getId(), currentUserId)
                .orElseThrow(() -> WorkspaceException.invitationForbidden(workspace.getId(), currentUserId));

        List<InvitationDraft> invitationDrafts = normalizeInvitationDrafts(request);

        assertRequesterCanInviteRoles(requestingMember.getRole(), invitationDrafts);

        List<String> requestedEmails = invitationDrafts.stream()
                .map(InvitationDraft::getEmail)
                .toList();

        List<String> activeMemberEmails = workspaceMemberRepository
                .findActiveMemberEmailsByWorkspaceIdAndEmails(workspace.getId(), requestedEmails);

        if (!activeMemberEmails.isEmpty()) {
            throw WorkspaceException.invitationUsersAlreadyMember(workspace.getId(), activeMemberEmails);
        }

        List<String> pendingInvitationEmails = workspaceMemberInviteRepository
                .findPendingInvitationEmailsByWorkspaceIdAndEmails(workspace.getId(), requestedEmails);

        Set<String> pendingInvitationEmailSet = pendingInvitationEmails.stream()
                .map(WorkspaceServiceImpl::normalizeEmail)
                .collect(Collectors.toUnmodifiableSet());

        List<WorkspaceMemberInvite> invitationsToCreate = invitationDrafts.stream()
                .filter(draft -> !pendingInvitationEmailSet.contains(draft.getEmail()))
                .map(draft -> WorkspaceMemberInvite.create(
                        WorkspaceMemberInviteCreateParam.builder()
                                .workspace(workspace)
                                .email(draft.getEmail())
                                .role(draft.getRole())
                                .token(tokenGenerator.generateBase64Url(TOKEN_BYTES))
                                .build()))
                .toList();

        if (invitationsToCreate.isEmpty()) {
            return SimpleMessageResponse.builder().message("Emails sent successfully").build();
        }

        try {
            List<WorkspaceMemberInvite> savedInvitations = workspaceMemberInviteRepository
                    .saveAllAndFlush(invitationsToCreate);

            for (WorkspaceMemberInvite invitation : savedInvitations) {
                EmailToSendEvent emailEvent = emailEventFactory.workspaceInvitation(workspace, invitation, currentUser);

                emailOutboxWriter.saveEmailToSendEvent(emailEvent, invitation.getId());
            }

            return SimpleMessageResponse.builder().message("Emails sent successfully").build();
        } catch (DataIntegrityViolationException exception) {
            throw WorkspaceException.invitationCreateFailed(exception);
        }
    }

    private static List<InvitationDraft> normalizeInvitationDrafts(WorkspaceInvitationRequest request) {
        Assert.notNull(request, "workspace invitation request is required");
        Assert.notEmpty(request.getEmails(), "workspace invitations emails are required");

        Map<String, InvitationDraft> draftsByEmail = new LinkedHashMap<>();

        for (WorkspaceInvitationRequest.EmailRoleRequest emailRoleRequest : request.getEmails()) {
            Assert.notNull(emailRoleRequest, "email role request is required");

            String email = normalizeEmail(emailRoleRequest.getEmail());
            WorkspaceRole role = Objects.requireNonNull(emailRoleRequest.getRole(), "role is required");

            InvitationDraft draft = InvitationDraft.of(email, role);
            InvitationDraft previous = draftsByEmail.putIfAbsent(email, draft);

            if (previous != null && previous.getRole() != role) {
                throw WorkspaceException.duplicateInvitationEmail(email);
            }
        }

        return List.copyOf(draftsByEmail.values());
    }

    private static void assertRequesterCanInviteRoles(WorkspaceRole requesterRole,
            List<InvitationDraft> invitationDrafts) {
        Assert.notNull(requesterRole, "requesterRole is required");
        Assert.notNull(invitationDrafts, "invitationDrafts is required");

        for (InvitationDraft draft : invitationDrafts) {
            if (!requesterRole.canInvite(draft.getRole())) {
                throw WorkspaceException.invitationRoleTooHigh(requesterRole, draft.getRole());
            }
        }
    }

    private static String normalizeEmail(String email) {
        Assert.hasText(email, "email is required");

        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static final class InvitationDraft {
        private final String email;
        private final WorkspaceRole role;

        private InvitationDraft(String email, WorkspaceRole role) {
            Assert.hasText(email, "email is required");

            this.email = email;
            this.role = Objects.requireNonNull(role, "role is required");
        }

        public static InvitationDraft of(String email, WorkspaceRole role) {
            return new InvitationDraft(email, role);
        }

        public String getEmail() {
            return email;
        }

        public WorkspaceRole getRole() {
            return role;
        }
    }

}
