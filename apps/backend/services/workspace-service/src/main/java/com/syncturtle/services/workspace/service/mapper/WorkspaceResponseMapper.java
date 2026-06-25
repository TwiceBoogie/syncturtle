package com.syncturtle.services.workspace.service.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import com.syncturtle.common.core.asset.AssetContentUrlFactory;
import com.syncturtle.services.workspace.dto.response.UserResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceInvitationWorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceMemberInvitationResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceInvitationProjection;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class WorkspaceResponseMapper {

    private final AssetContentUrlFactory assetUrlFactory;

    public WorkspaceResponse toResponse(CurrentUserWorkspaceProjection projection) {
        Assert.notNull(projection, "workspace projection is required");
        Assert.notNull(projection.getOwnerId(), "workspace owner projection is required");

        return WorkspaceResponse.builder()
                .id(projection.getId())
                .name(projection.getName())
                .slug(projection.getSlug())
                .role(projection.getRole())
                .totalMembers(projection.getTotalMembers())
                .organizationSize(projection.getOrganizationSize())
                .logo(assetUrlFactory.staticAssetUrl(projection.getLogoAssetId()))
                .logoAssetId(projection.getLogoAssetId())
                .owner(UserResponse.builder()
                        .id(projection.getOwnerId())
                        .displayName(projection.getOwnerDisplayName())
                        .email(projection.getOwnerEmail())
                        .firstName(projection.getOwnerFirstName())
                        .lastName(projection.getOwnerLastName())
                        .dateJoined(projection.getOwnerCreatedAt())
                        .avatarAssetId(projection.getOwnerAvatarAssetId())
                        .bot(projection.isBot())
                        .build())
                .createdById(projection.getCreatedById())
                .updatedById(projection.getUpdatedById())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .build();
    }

    public WorkspaceMemberInvitationResponse toWorkspaceMemberInvitationResponse(
            CurrentUserWorkspaceInvitationProjection projection) {
        return WorkspaceMemberInvitationResponse.builder()
                .id(projection.getId())
                .email(projection.getEmail())
                .accepted(projection.isAccepted())
                .message(projection.getMessage())
                .token(projection.getToken())
                .inviteLink(inviteLink(projection.getId(), projection.getEmail(), projection.getWorkspaceSlug()))
                .workspace(WorkspaceInvitationWorkspaceResponse.builder()
                        .id(projection.getWorkspaceId())
                        .name(projection.getWorkspaceName())
                        .slug(projection.getWorkspaceSlug())
                        .logoUrl(assetUrlFactory.staticAssetUrl(projection.getWorkspaceLogoAssetId()))
                        .build())
                .build();
    }

    private static String inviteLink(UUID invitationId, String email, String workspaceSlug) {
        return UriComponentsBuilder
                .fromPath("/workspace-invitations/")
                .queryParam("invitation_id", invitationId)
                .queryParam("email", email)
                .queryParam("slug", workspaceSlug)
                .build()
                .encode()
                .toUriString();
    }

}
