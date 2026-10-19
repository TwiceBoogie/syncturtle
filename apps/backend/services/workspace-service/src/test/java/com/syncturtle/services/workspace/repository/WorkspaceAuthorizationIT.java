package com.syncturtle.services.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.WorkspaceMember;
import com.syncturtle.services.workspace.model.param.WorkspaceCreateParam;
import com.syncturtle.services.workspace.model.param.WorkspaceMemberCreateParam;
import com.syncturtle.services.workspace.service.WorkspaceAuthorizationService;
import com.syncturtle.services.workspace.service.impl.WorkspaceAuthorizationServiceImpl;
import com.syncturtle.testing.annotation.JpaIntegrationTest;
import com.syncturtle.testing.annotation.UsePostgresDb;

import jakarta.persistence.EntityManager;

@EnableJpaAuditing
@JpaIntegrationTest
@UsePostgresDb("workspace_authorization_it")
@Import(WorkspaceAuthorizationServiceImpl.class)
class WorkspaceAuthorizationIT {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC);

    @Autowired
    WorkspaceRepository workspaces;

    @Autowired
    WorkspaceMemberRepository members;

    @Autowired
    WorkspaceAuthorizationService authorization;

    @Autowired
    EntityManager entityManager;

    @Nested
    class HasWorkspaceRoleAtLeast {

        @Test
        void activeOwnerHasAdminRoleOnlyInTheirWorkspace() {
            UUID ownerId = UUID.randomUUID();
            Workspace workspace = createWorkspace(ownerId);
            createMember(workspace, ownerId, WorkspaceRole.ADMIN);
            entityManager.clear();

            assertThat(authorization.hasWorkspaceRoleAtLeast(ownerId, workspace.getSlug(), WorkspaceRole.ADMIN)).isTrue();
            assertThat(authorization.isActiveWorkspaceMember(ownerId, workspace.getSlug())).isTrue();
            assertThat(authorization.hasWorkspaceRoleAtLeast(UUID.randomUUID(), workspace.getSlug(), WorkspaceRole.GUEST))
                    .isFalse();
            assertThat(authorization.hasWorkspaceRoleAtLeast(ownerId, "another-workspace", WorkspaceRole.GUEST))
                    .isFalse();
        }

        @Test
        void removedMemberLosesCurrentRoleAndMembership() {
            UUID userId = UUID.randomUUID();
            Workspace workspace = createWorkspace(userId);
            WorkspaceMember member = createMember(workspace, userId, WorkspaceRole.ADMIN);
            entityManager.clear();

            assertThat(authorization.hasWorkspaceRoleAtLeast(userId, workspace.getSlug(), WorkspaceRole.ADMIN)).isTrue();

            members.findById(member.getId()).orElseThrow().revoke(CLOCK);
            members.flush();
            entityManager.clear();

            assertThat(members.findActiveRoleByWorkspaceSlugAndUserId(workspace.getSlug(), userId)).isEmpty();
            assertThat(authorization.hasWorkspaceRoleAtLeast(userId, workspace.getSlug(), WorkspaceRole.GUEST)).isFalse();
            assertThat(authorization.isActiveWorkspaceMember(userId, workspace.getSlug())).isFalse();
            assertThat(members.findCurrentUserWorkspaces(userId)).isEmpty();
        }

        @Test
        void softDeletedMembershipCannotAuthorizeEvenIfLegacyRowRemainsActivated() {
            UUID userId = UUID.randomUUID();
            Workspace workspace = createWorkspace(userId);
            WorkspaceMember member = createMember(workspace, userId, WorkspaceRole.ADMIN);
            entityManager.clear();

            entityManager.createQuery("""
                    UPDATE WorkspaceMember member
                    SET member.deletedAt = :deletedAt
                    WHERE member.id = :memberId
                    """)
                    .setParameter("deletedAt", Instant.now(CLOCK))
                    .setParameter("memberId", member.getId())
                    .executeUpdate();
            entityManager.clear();

            assertThat(members.findById(member.getId()).orElseThrow().isActivated()).isTrue();
            assertThat(authorization.hasWorkspaceRoleAtLeast(userId, workspace.getSlug(), WorkspaceRole.ADMIN)).isFalse();
            assertThat(authorization.isActiveWorkspaceMember(userId, workspace.getSlug())).isFalse();
        }

        @Test
        void rejoiningAfterRemovalUsesOnlyTheNewMembershipRole() {
            UUID userId = UUID.randomUUID();
            Workspace workspace = createWorkspace(userId);
            WorkspaceMember formerAdmin = createMember(workspace, userId, WorkspaceRole.ADMIN);
            entityManager.clear();

            members.findById(formerAdmin.getId()).orElseThrow().revoke(CLOCK);
            members.flush();
            createMember(workspaces.findById(workspace.getId()).orElseThrow(), userId, WorkspaceRole.MEMBER);
            entityManager.clear();

            assertThat(members.findActiveRoleByWorkspaceSlugAndUserId(workspace.getSlug(), userId))
                    .contains(WorkspaceRole.MEMBER);
            assertThat(authorization.hasWorkspaceRoleAtLeast(userId, workspace.getSlug(), WorkspaceRole.ADMIN)).isFalse();
            assertThat(authorization.isActiveWorkspaceMember(userId, workspace.getSlug())).isTrue();
        }

        @Test
        void deletedWorkspaceCannotAuthorizeItsOwner() {
            UUID ownerId = UUID.randomUUID();
            Workspace workspace = createWorkspace(ownerId);
            createMember(workspace, ownerId, WorkspaceRole.ADMIN);
            entityManager.clear();

            workspaces.findById(workspace.getId()).orElseThrow().delete(CLOCK);
            workspaces.flush();
            entityManager.clear();

            assertThat(authorization.hasWorkspaceRoleAtLeast(ownerId, workspace.getSlug(), WorkspaceRole.ADMIN)).isFalse();
            assertThat(authorization.isActiveWorkspaceMember(ownerId, workspace.getSlug())).isFalse();
            assertThat(members.findCurrentUserWorkspaces(ownerId)).isEmpty();

            workspaces.findById(workspace.getId()).orElseThrow().restoreWorkspace();
            workspaces.flush();
            entityManager.clear();

            assertThat(authorization.hasWorkspaceRoleAtLeast(ownerId, workspace.getSlug(), WorkspaceRole.ADMIN)).isTrue();
            assertThat(authorization.isActiveWorkspaceMember(ownerId, workspace.getSlug())).isTrue();
        }

        @Test
        void deactivatedMemberCanAuthorizeAgainOnlyAfterReactivation() {
            UUID userId = UUID.randomUUID();
            Workspace workspace = createWorkspace(userId);
            WorkspaceMember member = createMember(workspace, userId, WorkspaceRole.MEMBER);
            entityManager.clear();

            members.findById(member.getId()).orElseThrow().deactivate();
            members.flush();
            entityManager.clear();

            assertThat(authorization.hasWorkspaceRoleAtLeast(userId, workspace.getSlug(), WorkspaceRole.MEMBER)).isFalse();
            assertThat(authorization.isActiveWorkspaceMember(userId, workspace.getSlug())).isFalse();

            members.findById(member.getId()).orElseThrow().activate();
            members.flush();
            entityManager.clear();

            assertThat(authorization.hasWorkspaceRoleAtLeast(userId, workspace.getSlug(), WorkspaceRole.MEMBER)).isTrue();
            assertThat(authorization.isActiveWorkspaceMember(userId, workspace.getSlug())).isTrue();
        }

        @Test
        void roleThresholdTracksCurrentRoleChanges() {
            UUID userId = UUID.randomUUID();
            Workspace workspace = createWorkspace(userId);
            WorkspaceMember member = createMember(workspace, userId, WorkspaceRole.MEMBER);
            entityManager.clear();

            assertThat(authorization.hasWorkspaceRoleAtLeast(userId, workspace.getSlug(), WorkspaceRole.MEMBER)).isTrue();
            assertThat(authorization.hasWorkspaceRoleAtLeast(userId, workspace.getSlug(), WorkspaceRole.ADMIN)).isFalse();

            members.findById(member.getId()).orElseThrow().changeRole(WorkspaceRole.ADMIN);
            members.flush();
            entityManager.clear();

            assertThat(authorization.hasWorkspaceRoleAtLeast(userId, workspace.getSlug(), WorkspaceRole.ADMIN)).isTrue();
        }
    }

    private Workspace createWorkspace(UUID ownerId) {
        String slug = "auth-" + UUID.randomUUID().toString().substring(0, 8);
        Workspace workspace = Workspace.create(WorkspaceCreateParam.builder()
                .name("Authorization test")
                .slug(slug)
                .ownerId(ownerId)
                .build());
        return workspaces.saveAndFlush(workspace);
    }

    private WorkspaceMember createMember(Workspace workspace, UUID userId, WorkspaceRole role) {
        WorkspaceMember member = WorkspaceMember.create(WorkspaceMemberCreateParam.builder()
                .workspace(workspace)
                .memberId(userId)
                .role(role)
                .build());
        return members.saveAndFlush(member);
    }
}
