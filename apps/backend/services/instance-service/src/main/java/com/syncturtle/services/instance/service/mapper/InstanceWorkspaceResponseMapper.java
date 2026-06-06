package com.syncturtle.services.instance.service.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.services.instance.dto.response.InstanceWorkspaceOwnerResponse;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceResponse;
import com.syncturtle.services.instance.repository.projection.InstanceWorkspaceOwnerProjection;
import com.syncturtle.services.instance.repository.projection.InstanceWorkspaceProjection;
import com.syncturtle.services.instance.service.asset.WorkspaceAssetUrlFactory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class InstanceWorkspaceResponseMapper {

    private final WorkspaceAssetUrlFactory assetUrlFactory;

    public InstanceWorkspaceResponse toResponse(InstanceWorkspaceProjection projection) {
        Assert.notNull(projection, "workspace projection is required");

        return InstanceWorkspaceResponse.builder()
                .id(projection.getId())
                .name(projection.getName())
                .logo(assetUrlFactory.logoContentUrl(projection.getLogoAssetId()))
                .logoAssetId(projection.getLogoAssetId())
                .slug(projection.getSlug())
                .organizationSize(projection.getOrganizationSize())
                .owner(toOwnerResponse(projection.getOwner()))
                .totalMembers(projection.getTotalMembers())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .createdById(projection.getCreatedById())
                .updatedById(projection.getUpdatedById())
                .build();
    }

    private InstanceWorkspaceOwnerResponse toOwnerResponse(InstanceWorkspaceOwnerProjection projection) {
        // allow owner to be nullable becase even ordering can happen
        // WorkspaceEvent arrives before UserEvent
        if (projection == null) {
            return null;
        }

        return InstanceWorkspaceOwnerResponse.builder()
                .id(projection.getId())
                .username(projection.getUsername())
                .email(projection.getEmail())
                .displayName(projection.getDisplayName())
                .firstName(projection.getFirstName())
                .lastName(projection.getLastName())
                .dateJoined(projection.getCreatedAt())
                .avatarAssetId(projection.getAvatarAssetId())
                .coverImageAssetId(projection.getCoverImageAssetId())
                .emailVerified(projection.isEmailVerified())
                .passwordAutoset(projection.isPasswordAutoset())
                .userTimezone(projection.getUserTimezone())
                .bot(projection.isBot())
                .build();
    }

}
