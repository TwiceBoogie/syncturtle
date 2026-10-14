package com.syncturtle.services.workspace.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.services.workspace.repository.WorkspaceMemberRepository;

class WorkspaceAuthorizationServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("f7e6fc27-6a58-4b4b-9215-d6a191c1def1");
    private static final String SLUG = "test-workspace";

    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final WorkspaceAuthorizationServiceImpl authorization = new WorkspaceAuthorizationServiceImpl(members);

    @Nested
    class HasWorkspaceRoleAtLeast {

        @Test
        void deniesMissingMembershipAndInsufficientRole() {
            when(members.findActiveRoleByWorkspaceSlugAndUserId(SLUG, USER_ID))
                    .thenAnswer(invocation -> Optional.empty())
                    .thenAnswer(invocation -> Optional.of(WorkspaceRole.MEMBER));

            assertThat(authorization.hasWorkspaceRoleAtLeast(USER_ID, SLUG, WorkspaceRole.ADMIN)).isFalse();
            assertThat(authorization.hasWorkspaceRoleAtLeast(USER_ID, SLUG, WorkspaceRole.ADMIN)).isFalse();
        }

        @Test
        void repositoryFailureDoesNotGrantAccess() {
            DataAccessResourceFailureException failure = new DataAccessResourceFailureException("database unavailable");
            when(members.findActiveRoleByWorkspaceSlugAndUserId(SLUG, USER_ID)).thenThrow(failure);

            assertThatThrownBy(() -> authorization.hasWorkspaceRoleAtLeast(USER_ID, SLUG, WorkspaceRole.ADMIN))
                    .isSameAs(failure);
        }
    }

    @Nested
    class IsActiveWorkspaceMember {

        @Test
        void repositoryFailureDoesNotGrantAccess() {
            DataAccessResourceFailureException failure = new DataAccessResourceFailureException("database unavailable");
            when(members.existsByWorkspaceSlugAndUserIdAndActiveTrue(SLUG, USER_ID)).thenThrow(failure);

            assertThatThrownBy(() -> authorization.isActiveWorkspaceMember(USER_ID, SLUG)).isSameAs(failure);
        }
    }
}
