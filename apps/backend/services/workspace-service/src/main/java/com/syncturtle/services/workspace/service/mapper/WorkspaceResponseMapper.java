package com.syncturtle.services.workspace.service.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.repository.projection.WorkspaceProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class WorkspaceResponseMapper {

    private final UserResponseMapper userResponseMapper;

    public WorkspaceResponse toResponse(WorkspaceProjection projection) {
        Assert.notNull(projection, "workspace projection is required");
        Assert.notNull(projection.getOwner(), "workspace owner projection is required");

        return WorkspaceResponse.builder()
                .id(projection.getId())
                .name(projection.getName())
                // .logo(projection.getLogo())
                .logoAssetId(projection.getLogoAssetId())
                .slug(projection.getSlug())
                .organizationSize(projection.getOrganizationSize())
                .owner(userResponseMapper.toResponse(projection.getOwner()))
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .createdById(projection.getCreatedById())
                .updatedById(projection.getUpdatedById())
                .build();
    }

}
