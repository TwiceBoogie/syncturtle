package com.syncturtle.services.workspace.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.syncturtle.common.contracts.workspace.error.WorkspaceErrorCode;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceMemberEvent;
import com.syncturtle.common.contracts.workspace.exception.WorkspaceException;
import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.request.WorkspaceInvitationRequest;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.mapper.WorkspaceApiMapper;
import com.syncturtle.services.workspace.messaging.kafka.factory.WorkspaceEventFactory;
import com.syncturtle.services.workspace.messaging.kafka.factory.WorkspaceInvitationEmailEventFactory;
import com.syncturtle.services.workspace.model.UserLite;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.WorkspaceMember;
import com.syncturtle.services.workspace.model.param.WorkspaceCreateParam;
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

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class WorkspacePersonalFirstCreationTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SPACE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant NOW = Instant.parse("2026-09-29T12:00:00Z");

    @Mock
    WorkspaceRepository workspaces;
    @Mock
    WorkspaceMemberRepository members;
    @Mock
    WorkspaceMemberInviteRepository invitations;
    @Mock
    UserLiteRepository users;
    @Mock
    WorkspaceConfigResolver flags;
    @Mock
    WorkspaceInvitationEmailEventFactory emails;
    @Mock
    EmailOutboxWriter emailOutbox;
    @Mock
    WorkspaceOutboxWriter outbox;
    @Mock
    SecureTokenGenerator tokens;
    @Mock
    WorkspaceApiMapper mapper;
    private WorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceServiceImpl(workspaces, members, invitations, users, flags,
                new WorkspaceEventFactory(Clock.fixed(NOW, ZoneOffset.UTC)), emails,
                emailOutbox, outbox, new WorkspaceSlugPolicy(workspaces), tokens, mapper);
    }

    @Nested
    class WorkspaceCreate {
        @Test
        void createsWithoutOrganizationAndPublishesOwnerAdminAndCompatibleEvents() throws Exception {
            // arrange
            WorkspaceCreateRequest request = WorkspaceCreateRequest.builder().name("Personal").slug("personal-1234")
                    .build();
            CurrentUserWorkspaceProjection projection = mock(CurrentUserWorkspaceProjection.class);
            WorkspaceResponse expected = WorkspaceResponse.builder().id(SPACE).slug("personal-1234").role(20).build();
            // conditions
            when(flags.getInstanceConfigurations()).thenReturn(WorkspaceFlagRuntimeSnapshot.builder().build());
            when(workspaces.saveAndFlush(any(Workspace.class))).thenAnswer(invocation -> {
                Workspace space = invocation.getArgument(0);
                ReflectionTestUtils.setField(space, "id", SPACE);
                ReflectionTestUtils.setField(space, "createdAt", NOW);
                ReflectionTestUtils.setField(space, "updatedAt", NOW);
                ReflectionTestUtils.setField(space, "version", 0L);
                return space;
            });
            when(members.saveAndFlush(any(WorkspaceMember.class))).thenAnswer(invocation -> {
                WorkspaceMember member = invocation.getArgument(0);
                ReflectionTestUtils.setField(member, "id", UUID.fromString("33333333-3333-3333-3333-333333333333"));
                ReflectionTestUtils.setField(member, "createdAt", NOW);
                ReflectionTestUtils.setField(member, "updatedAt", NOW);
                ReflectionTestUtils.setField(member, "version", 0L);
                return member;
            });
            when(members.findCurrentUserWorkspaceById(USER, SPACE)).thenReturn(Optional.of(projection));
            when(mapper.toWorkspaceResponse(projection)).thenReturn(expected);
            // act
            WorkspaceResponse response = service.workspaceCreate(USER, request);
            // assert
            ArgumentCaptor<Workspace> spaceCaptor = ArgumentCaptor.forClass(Workspace.class);
            ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
            verify(workspaces).saveAndFlush(spaceCaptor.capture());
            verify(members).saveAndFlush(memberCaptor.capture());
            assertThat(spaceCaptor.getValue().getOrganizationSize()).isNull();
            assertThat(spaceCaptor.getValue().getOwnerId()).isEqualTo(USER);
            assertThat(memberCaptor.getValue().getRole()).isEqualTo(WorkspaceRole.ADMIN);
            assertThat(memberCaptor.getValue().getMemberId()).isEqualTo(USER);
            assertThat(memberCaptor.getValue().isActive()).isTrue();
            assertThat(memberCaptor.getValue().getCompanyRole()).isNull();
            assertThat(response).isSameAs(expected);
            // Important interactions: both integration events, no invitation.
            ArgumentCaptor<WorkspaceEvent> eventCaptor = ArgumentCaptor.forClass(WorkspaceEvent.class);
            ArgumentCaptor<WorkspaceMemberEvent> memberEventCaptor = ArgumentCaptor
                    .forClass(WorkspaceMemberEvent.class);
            verify(outbox).saveWorkspaceEvent(eventCaptor.capture());
            verify(outbox).saveWorkspaceMemberEvent(memberEventCaptor.capture());
            WorkspaceEvent event = eventCaptor.getValue();
            assertThat(event.getOrganizationSize()).isNull();
            assertThat(event.getType()).isEqualTo(WorkspaceEvent.Type.WORKSPACE_CREATED);
            assertThat(memberEventCaptor.getValue().getRole()).isEqualTo(20);
            assertThat(memberEventCaptor.getValue().getWorkspaceId()).isEqualTo(SPACE);
            JsonMapper json = new JsonMapper();
            WorkspaceEvent roundTrip = json.readValue(json.writeValueAsString(event), WorkspaceEvent.class);
            assertThat(roundTrip.getId()).isEqualTo(SPACE);
            assertThat(roundTrip.getOrganizationSize()).isNull();
            assertThat(roundTrip.getOwnerId()).isEqualTo(USER);
            verifyNoInteractions(invitations, emailOutbox);
        }

        @Test
        void creationDisabledDoesNotProvisionAnything() {
            WorkspaceCreateRequest request = WorkspaceCreateRequest.builder().name("Personal").slug("personal-1234")
                    .build();
            when(flags.getInstanceConfigurations())
                    .thenReturn(WorkspaceFlagRuntimeSnapshot.builder().workspaceCreationDisabled(true).build());
            assertThatThrownBy(() -> service.workspaceCreate(USER, request)).isInstanceOf(WorkspaceException.class);
            verifyNoInteractions(workspaces, members, outbox);
        }

        @Test
        void reservedSlugStillRejectsBeforePersistence() {
            WorkspaceCreateRequest request = WorkspaceCreateRequest.builder().name("Personal").slug("api").build();
            when(flags.getInstanceConfigurations()).thenReturn(WorkspaceFlagRuntimeSnapshot.builder().build());
            assertThatThrownBy(() -> service.workspaceCreate(USER, request))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(workspaces, members, outbox);
        }
    }

    @Nested
    class SlugCheck {
        @Test
        void takenSlugReturnsUnavailableForOrdinaryAuthenticatedEndpoint() {
            when(workspaces.existsBySlugIgnoreCaseAndDeletedAtIsNull("personal-1234")).thenReturn(true);
            assertThat(service.workspaceSlugCheck("personal-1234").isAvailable()).isFalse();
            verifyNoInteractions(members, outbox);
        }
    }

    @Nested
    class CreateWorkspaceInvitations {

        @Test
        void removedMemberCannotCreateInvitations() {
            Workspace workspace = invitationWorkspace();
            WorkspaceInvitationRequest request = invitationRequest(WorkspaceRole.MEMBER);
            when(workspaces.findBySlugAndDeletedAtIsNull("personal-1234")).thenReturn(Optional.of(workspace));
            when(users.findById(USER)).thenReturn(Optional.of(mock(UserLite.class)));
            when(members.findActiveRoleByWorkspaceSlugAndUserId("personal-1234", USER))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createWorkspaceInvitations(USER, "personal-1234", request))
                    .isInstanceOfSatisfying(WorkspaceException.class, exception -> assertThat(exception.getWorkspaceErrorCode())
                            .isEqualTo(WorkspaceErrorCode.WORKSPACE_INVITATION_FORBIDDEN));
            verifyNoInteractions(invitations, tokens, emails, emailOutbox, outbox);
        }

        @Test
        void currentRoleStillLimitsInvitedRoles() {
            Workspace workspace = invitationWorkspace();
            WorkspaceInvitationRequest request = invitationRequest(WorkspaceRole.ADMIN);
            when(workspaces.findBySlugAndDeletedAtIsNull("personal-1234")).thenReturn(Optional.of(workspace));
            when(users.findById(USER)).thenReturn(Optional.of(mock(UserLite.class)));
            when(members.findActiveRoleByWorkspaceSlugAndUserId("personal-1234", USER))
                    .thenReturn(Optional.of(WorkspaceRole.MEMBER));

            assertThatThrownBy(() -> service.createWorkspaceInvitations(USER, "personal-1234", request))
                    .isInstanceOfSatisfying(WorkspaceException.class, exception -> assertThat(exception.getWorkspaceErrorCode())
                            .isEqualTo(WorkspaceErrorCode.WORKSPACE_INVITATION_ROLE_TOO_HIGH));
            verifyNoInteractions(invitations, tokens, emails, emailOutbox, outbox);
        }

        @Test
        void repositoryFailureCannotCreateInvitations() {
            Workspace workspace = invitationWorkspace();
            WorkspaceInvitationRequest request = invitationRequest(WorkspaceRole.MEMBER);
            DataAccessResourceFailureException failure = new DataAccessResourceFailureException("database unavailable");
            when(workspaces.findBySlugAndDeletedAtIsNull("personal-1234")).thenReturn(Optional.of(workspace));
            when(users.findById(USER)).thenReturn(Optional.of(mock(UserLite.class)));
            when(members.findActiveRoleByWorkspaceSlugAndUserId("personal-1234", USER)).thenThrow(failure);

            assertThatThrownBy(() -> service.createWorkspaceInvitations(USER, "personal-1234", request))
                    .isSameAs(failure);
            verifyNoInteractions(invitations, tokens, emails, emailOutbox, outbox);
        }

        @Test
        void currentAdminCanUseExistingInvitationFlow() {
            Workspace workspace = invitationWorkspace();
            WorkspaceInvitationRequest request = invitationRequest(WorkspaceRole.MEMBER);
            when(workspaces.findBySlugAndDeletedAtIsNull("personal-1234")).thenReturn(Optional.of(workspace));
            when(users.findById(USER)).thenReturn(Optional.of(mock(UserLite.class)));
            when(members.findActiveRoleByWorkspaceSlugAndUserId("personal-1234", USER))
                    .thenReturn(Optional.of(WorkspaceRole.ADMIN));
            when(members.findActiveMemberEmailsByWorkspaceIdAndEmails(SPACE, List.of("invitee@example.com")))
                    .thenReturn(List.of());
            when(invitations.findPendingInvitationEmailsByWorkspaceIdAndEmails(SPACE, List.of("invitee@example.com")))
                    .thenReturn(List.of("invitee@example.com"));

            assertThat(service.createWorkspaceInvitations(USER, "personal-1234", request).getMessage())
                    .isEqualTo("Emails sent successfully");
            verifyNoInteractions(tokens, emails, emailOutbox, outbox);
        }

        private Workspace invitationWorkspace() {
            Workspace workspace = Workspace.create(WorkspaceCreateParam.builder()
                    .name("Personal").slug("personal-1234").ownerId(USER).build());
            ReflectionTestUtils.setField(workspace, "id", SPACE);
            return workspace;
        }

        private WorkspaceInvitationRequest invitationRequest(WorkspaceRole role) {
            WorkspaceInvitationRequest.EmailRoleRequest invitee = WorkspaceInvitationRequest.EmailRoleRequest.builder()
                    .email("invitee@example.com")
                    .role(role)
                    .build();
            return WorkspaceInvitationRequest.builder().emails(List.of(invitee)).build();
        }
    }

}
