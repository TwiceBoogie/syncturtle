package com.syncturtle.services.workspace.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import com.syncturtle.common.contracts.workspace.error.WorkspaceErrorCode;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.exception.WorkspaceException;
import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.services.workspace.dto.request.WorkspaceLogoUpdateRequest;
import com.syncturtle.services.workspace.messaging.kafka.factory.WorkspaceEventFactory;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.param.WorkspaceCreateParam;
import com.syncturtle.services.workspace.repository.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.repository.WorkspaceRepository;
import com.syncturtle.services.workspace.service.collaborator.asset.WorkspaceLogoAssetVerifier;
import com.syncturtle.services.workspace.service.collaborator.outbox.WorkspaceOutboxWriter;

class WorkspaceAssetAuthorizationTest {

    private static final UUID USER_ID = UUID.fromString("f7e6fc27-6a58-4b4b-9215-d6a191c1def1");
    private static final String SLUG = "test-workspace";

    private final WorkspaceRepository workspaces = mock(WorkspaceRepository.class);
    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final WorkspaceLogoAssetVerifier logoVerifier = mock(WorkspaceLogoAssetVerifier.class);
    private final WorkspaceEventFactory events = mock(WorkspaceEventFactory.class);
    private final WorkspaceOutboxWriter outbox = mock(WorkspaceOutboxWriter.class);
    private final WorkspaceAssetServiceImpl service = new WorkspaceAssetServiceImpl(
            workspaces, members, logoVerifier, events, outbox);

    @Nested
    class ClearLogo {

        @Test
        void removedMemberCannotChangeLogo() {
            Workspace workspace = workspace();
            when(workspaces.findBySlugAndDeletedAtIsNull(SLUG)).thenReturn(Optional.of(workspace));
            when(members.findActiveRoleByWorkspaceSlugAndUserId(SLUG, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.clearLogo(USER_ID, SLUG))
                    .isInstanceOfSatisfying(WorkspaceException.class, exception ->
                            assertThat(exception.getWorkspaceErrorCode())
                                    .isEqualTo(WorkspaceErrorCode.WORKSPACE_OWNER_NOT_FOUND));
            verify(workspaces, never()).flush();
            verifyNoInteractions(logoVerifier, events, outbox);
        }

        @Test
        void nonAdminCannotChangeLogo() {
            Workspace workspace = workspace();
            when(workspaces.findBySlugAndDeletedAtIsNull(SLUG)).thenReturn(Optional.of(workspace));
            when(members.findActiveRoleByWorkspaceSlugAndUserId(SLUG, USER_ID))
                    .thenReturn(Optional.of(WorkspaceRole.MEMBER));

            assertThatThrownBy(() -> service.clearLogo(USER_ID, SLUG))
                    .isInstanceOf(WorkspaceException.class);
            verify(workspaces, never()).flush();
            verifyNoInteractions(logoVerifier, events, outbox);
        }

        @Test
        void deletedWorkspaceCannotChangeLogo() {
            when(workspaces.findBySlugAndDeletedAtIsNull(SLUG)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.clearLogo(USER_ID, SLUG))
                    .isInstanceOfSatisfying(WorkspaceException.class, exception ->
                            assertThat(exception.getWorkspaceErrorCode())
                                    .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
            verifyNoInteractions(members, logoVerifier, events, outbox);
        }

        @Test
        void activeAdminCanChangeLogo() {
            Workspace workspace = workspace();
            WorkspaceEvent event = mock(WorkspaceEvent.class);
            when(workspaces.findBySlugAndDeletedAtIsNull(SLUG)).thenReturn(Optional.of(workspace));
            when(members.findActiveRoleByWorkspaceSlugAndUserId(SLUG, USER_ID))
                    .thenReturn(Optional.of(WorkspaceRole.ADMIN));
            when(events.updated(workspace)).thenReturn(event);

            service.clearLogo(USER_ID, SLUG);

            verify(workspaces).flush();
            verify(outbox).saveWorkspaceEvent(event);
        }

        @Test
        void repositoryFailureDoesNotChangeLogo() {
            Workspace workspace = workspace();
            DataAccessResourceFailureException failure = new DataAccessResourceFailureException("database unavailable");
            when(workspaces.findBySlugAndDeletedAtIsNull(SLUG)).thenReturn(Optional.of(workspace));
            when(members.findActiveRoleByWorkspaceSlugAndUserId(SLUG, USER_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.clearLogo(USER_ID, SLUG)).isSameAs(failure);
            verify(workspaces, never()).flush();
            verifyNoInteractions(logoVerifier, events, outbox);
        }
    }

    @Test
    void removedMemberCannotUpdateLogoOrReachAssetVerifier() {
        Workspace workspace = workspace();
        WorkspaceLogoUpdateRequest request = WorkspaceLogoUpdateRequest.builder().assetId(UUID.randomUUID()).build();
        when(workspaces.findBySlugAndDeletedAtIsNull(SLUG)).thenReturn(Optional.of(workspace));
        when(members.findActiveRoleByWorkspaceSlugAndUserId(SLUG, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateLogo(USER_ID, SLUG, request))
                .isInstanceOf(WorkspaceException.class);
        verify(workspaces, never()).flush();
        verifyNoInteractions(logoVerifier, events, outbox);
    }

    private Workspace workspace() {
        return Workspace.create(WorkspaceCreateParam.builder()
                .name("Test workspace")
                .slug(SLUG)
                .ownerId(USER_ID)
                .build());
    }
}
