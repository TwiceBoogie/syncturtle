package com.syncturtle.services.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.model.WorkspaceMember;
import com.syncturtle.services.workspace.model.param.WorkspaceCreateParam;
import com.syncturtle.services.workspace.model.param.WorkspaceMemberCreateParam;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceProjection;
import com.syncturtle.testing.annotation.JpaIntegrationTest;
import com.syncturtle.testing.annotation.UsePostgresDb;

import jakarta.persistence.EntityManager;

@EnableJpaAuditing
@JpaIntegrationTest
@UsePostgresDb("workspace_personal_first_it")
class WorkspacePersonalFirstPersitenceIT {

    @Autowired
    WorkspaceRepository workspaces;
    @Autowired
    WorkspaceMemberRepository members;
    @Autowired
    EntityManager entityManager;

    @Test
    void nullOrganizationPersistsAndOneOwnerMembershipIsUsable() {
        // arrange
        UUID owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Workspace space = Workspace.create(WorkspaceCreateParam.builder().name("Personal")
                .slug("personal-" + UUID.randomUUID().toString().substring(0, 8)).ownerId(owner).build());
        Workspace saved = workspaces.saveAndFlush(space);
        WorkspaceMember member = WorkspaceMember.create(WorkspaceMemberCreateParam.builder()
                .workspace(saved)
                .memberId(owner)
                .role(WorkspaceRole.ADMIN)
                .build());
        // act
        members.saveAndFlush(member);
        UUID id = saved.getId();
        entityManager.clear();
        // assert
        assertThat(workspaces.findById(id).orElseThrow().getOrganizationSize()).isNull();
        List<CurrentUserWorkspaceProjection> destinations = members.findCurrentUserWorkspaces(owner);
        assertThat(destinations).hasSize(1);
        assertThat(destinations.get(0).getId()).isEqualTo(id);
        assertThat(destinations.get(0).getRole()).isEqualTo(20);
        assertThat(destinations.get(0).getOwnerId()).isEqualTo(owner);
        assertThat(destinations.get(0).getTotalMembers()).isEqualTo(1);
    }

}
