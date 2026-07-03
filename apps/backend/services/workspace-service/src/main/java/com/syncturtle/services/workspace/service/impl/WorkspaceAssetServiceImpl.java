package com.syncturtle.services.workspace.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.error.WorkspaceErrorCode;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.common.contracts.workspace.exception.WorkspaceException;
import com.syncturtle.services.workspace.dto.request.WorkspaceLogoUpdateRequest;
import com.syncturtle.services.workspace.messaging.kafka.factory.WorkspaceEventFactory;
import com.syncturtle.services.workspace.messaging.outbox.WorkspaceOutboxWriter;
import com.syncturtle.services.workspace.model.Workspace;
import com.syncturtle.services.workspace.repository.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.repository.WorkspaceRepository;
import com.syncturtle.services.workspace.service.WorkspaceAssetService;
import com.syncturtle.services.workspace.service.asset.WorkspaceLogoAssetVerifier;
import com.syncturtle.services.workspace.type.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceAssetServiceImpl implements WorkspaceAssetService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceLogoAssetVerifier logoAssetVerifier;
    private final WorkspaceEventFactory eventFactory;
    private final WorkspaceOutboxWriter outboxWriter;

    @Override
    @Transactional
    public void updateLogo(UUID currentUserId, String workspaceSlug, WorkspaceLogoUpdateRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(request, "workspace logo update request is required");

        Workspace workspace = requireWorkspace(workspaceSlug);
        requireWorkspaceAdmin(currentUserId, workspace.getId());
        logoAssetVerifier.requireValidLogoAsset(request.getAssetId(), workspace.getId(), currentUserId);

        workspace.updateLogo(request.getAssetId());

        WorkspaceEvent event = eventFactory.updated(workspace);
        outboxWriter.saveWorkspaceEvent(event);
    }

    @Override
    @Transactional
    public void clearLogo(UUID currentUserId, String workspaceSlug) {
        Assert.notNull(currentUserId, "currentUserId is required");

        Workspace workspace = requireWorkspace(workspaceSlug);
        requireWorkspaceAdmin(currentUserId, workspace.getId());

        workspace.clearLogo();

        WorkspaceEvent event = eventFactory.updated(workspace);
        outboxWriter.saveWorkspaceEvent(event);
    }

    private Workspace requireWorkspace(String workspaceSlug) {
        Assert.hasText(workspaceSlug, "workspaceSlug is required");

        return workspaceRepository.findBySlugAndDeletedAtIsNull(workspaceSlug)
                .orElseThrow(() -> WorkspaceException.of(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
    }

    private void requireWorkspaceAdmin(UUID currentUserId, UUID workspaceId) {
        boolean allowed = workspaceMemberRepository.existsByMemberIdAndWorkspace_IdAndRoleAndDeletedAtIsNull(
                currentUserId, workspaceId, WorkspaceRole.ADMIN);

        if (!allowed) {
            throw WorkspaceException.of(WorkspaceErrorCode.WORKSPACE_OWNER_NOT_FOUND);
        }
    }

}
