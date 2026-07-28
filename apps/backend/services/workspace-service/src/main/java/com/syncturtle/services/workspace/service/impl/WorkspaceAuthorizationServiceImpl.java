package com.syncturtle.services.workspace.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.services.workspace.repository.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.service.WorkspaceAuthorizationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceAuthorizationServiceImpl implements WorkspaceAuthorizationService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasWorkspaceRoleAtLeast(UUID userId, String workspaceSlug, WorkspaceRole requiredRole) {
        Assert.notNull(userId, "userId is required");
        Assert.hasText(workspaceSlug, "workspaceSlug is required");
        Assert.notNull(requiredRole, "requiredRole is required");

        return workspaceMemberRepository.findActiveRoleByWorkspaceSlugAndUserId(workspaceSlug, userId)
                .map(actualRole -> actualRole.isAtLeast(requiredRole))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveWorkspaceMember(UUID userId, String workspaceSlug) {
        Assert.notNull(userId, "userId is required");
        Assert.hasText(workspaceSlug, "workspaceSlug is required");

        return workspaceMemberRepository.existsByWorkspaceSlugAndUserIdAndActiveTrue(workspaceSlug, userId);
    }

}
