package com.syncturtle.services.workspace.service.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.services.workspace.dto.response.UserResponse;
import com.syncturtle.services.workspace.repository.projection.UserProjection;

@Component
public final class UserResponseMapper {

    public UserResponse toResponse(UserProjection projection) {
        Assert.notNull(projection, "user projection is required");

        return UserResponse.builder()
                .id(projection.getId())
                .username(projection.getUsername())
                .email(projection.getEmail())
                .displayName(projection.getDisplayName())
                .firstName(projection.getFirstName())
                .lastName(projection.getLastName())
                .dateJoined(projection.getCreatedAt())
                .avatarAssetId(projection.getAvatarAssetId())
                .coverImageAssetId(projection.getCoverImageAssetId())
                .active(projection.isActive())
                .emailVerified(projection.isEmailVerified())
                .passwordAutoset(projection.isPasswordAutoset())
                .timezone(projection.getUserTimezone())
                .bot(projection.isBot())
                .build();
    }

}
