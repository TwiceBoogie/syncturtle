package com.syncturtle.services.instance.service.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.core.actor.PrincipalType;
import com.syncturtle.common.core.asset.AssetContentUrlFactory;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceOwnerResponse;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceResponse;
import com.syncturtle.services.instance.repository.projection.InstanceWorkspaceProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceWorkspaceResponseMapper {

    private final AssetContentUrlFactory assetUrlFactory;

    public InstanceWorkspaceResponse toResponse(InstanceWorkspaceProjection projection) {
        Assert.notNull(projection, "workspace projection is required");

        return InstanceWorkspaceResponse.builder()
                .id(projection.getId())
                .name(projection.getName())
                .logo(assetUrlFactory.staticAssetUrl(projection.getLogoAssetId()))
                .logoAssetId(projection.getLogoAssetId())
                .slug(projection.getSlug())
                .organizationSize(projection.getOrganizationSize())
                .owner(toOwnerResponse(projection))
                .totalMembers(projection.getTotalMembers() == null ? 0L : projection.getTotalMembers())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .createdById(projection.getCreatedById())
                .updatedById(projection.getUpdatedById())
                .build();
    }

    private InstanceWorkspaceOwnerResponse toOwnerResponse(InstanceWorkspaceProjection projection) {
        // allow owner to be nullable becase even ordering can happen
        // WorkspaceEvent arrives before UserEvent
        if (projection.getOwnerId() == null) {
            return null;
        }

        return InstanceWorkspaceOwnerResponse.builder()
                .id(projection.getOwnerId())
                .username(projection.getOwnerUsername())
                .email(projection.getOwnerEmail())
                .displayName(projection.getOwnerDisplayName())
                .firstName(projection.getOwnerFirstName())
                .lastName(projection.getOwnerLastName())
                .dateJoined(projection.getOwnerCreatedAt())
                .avatarAssetId(projection.getOwnerAvatarAssetId())
                .coverImageAssetId(projection.getOwnerCoverImageAssetId())
                .emailVerified(Boolean.TRUE.equals(projection.getOwnerEmailVerified()))
                .passwordAutoset(Boolean.TRUE.equals(projection.getOwnerPasswordAutoset()))
                .userTimezone(projection.getOwnerUserTimezone())
                .bot(PrincipalType.BOT.name().equals(projection.getOwnerPrincipalType()))
                .build();
    }

}
