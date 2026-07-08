package com.syncturtle.services.instance.service.mapper;

import org.springframework.stereotype.Component;

import com.syncturtle.common.core.asset.AssetContentUrlFactory;
import com.syncturtle.services.instance.dto.response.InstanceAdminMeResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminSessionResponse;
import com.syncturtle.services.instance.dto.response.UserAdminLiteResponse;
import com.syncturtle.services.instance.model.InstanceAdmin;
import com.syncturtle.services.instance.model.UserLite;
import com.syncturtle.services.instance.repository.projection.AdminUserDetailLiteProjection;
import com.syncturtle.services.instance.repository.projection.AdminUserDetailsProjection;
import com.syncturtle.services.instance.repository.projection.InstanceAdminProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstanceAdminApiMapper {

    private final AssetContentUrlFactory assetUrlFactory;

    public InstanceAdminResponse toResponse(InstanceAdminProjection instanceAdmin,
            AdminUserDetailLiteProjection userDetail) {
        return InstanceAdminResponse.builder()
                .id(instanceAdmin.getId())
                .instance(instanceAdmin.getInstance() == null ? null : instanceAdmin.getInstance().getId())
                .user(instanceAdmin.getUserId())
                .role(instanceAdmin.getRole())
                .createdAt(instanceAdmin.getCreatedAt())
                .updatedAt(instanceAdmin.getUpdatedAt())
                .userDetail(toUserDetails(userDetail))
                .createdBy(instanceAdmin.getCreatedById())
                .updatedBy(instanceAdmin.getUpdatedById())
                .build();
    }

    public InstanceAdminResponse toResponse(InstanceAdmin instanceAdmin, AdminUserDetailLiteProjection user) {
        return InstanceAdminResponse.builder()
                .id(instanceAdmin.getId())
                .instance(instanceAdmin.getInstance() == null ? null : instanceAdmin.getInstance().getId())
                .user(instanceAdmin.getUserId())
                .role(instanceAdmin.getRole())
                .createdAt(instanceAdmin.getCreatedAt())
                .updatedAt(instanceAdmin.getUpdatedAt())
                .userDetail(toUserDetails(user))
                .createdBy(instanceAdmin.getCreatedById())
                .updatedBy(instanceAdmin.getUpdatedById())
                .build();
    }

    public InstanceAdminMeResponse toMeResponse(AdminUserDetailsProjection instanceAdmin) {
        return InstanceAdminMeResponse.builder()
                .id(instanceAdmin.getId())
                .username(instanceAdmin.getUsername())
                .email(instanceAdmin.getEmail())
                .displayName(instanceAdmin.getDisplayName())
                .firstName(instanceAdmin.getFirstName())
                .lastName(instanceAdmin.getLastName())
                .userTimezone(instanceAdmin.getUserTimezone())
                .avatar(instanceAdmin.getAvatarAssetId())
                .avatarUrl(assetUrlFactory.staticAssetUrl(instanceAdmin.getAvatarAssetId()))
                .coverImage(instanceAdmin.getCoverImageAssetId())
                .coverImageUrl(assetUrlFactory.staticAssetUrl(instanceAdmin.getCoverImageAssetId()))
                .emailVerified(instanceAdmin.isEmailVerified())
                .passwordAutoset(instanceAdmin.isPasswordAutoset())
                .bot(instanceAdmin.isBot())
                .lastLoginMedium(instanceAdmin.getLastLoginMedium())
                .dateJoined(instanceAdmin.getCreatedAt())
                .build();
    }

    public InstanceAdminSessionResponse toSessionResponse(AdminUserDetailsProjection instanceAdmin) {
        return InstanceAdminSessionResponse.authenticated(toMeResponse(instanceAdmin));
    }

    public UserAdminLiteResponse toUserAdminLiteResponse(UserLite user) {
        return UserAdminLiteResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarAssetId().toString())
                .dateJoined(user.getCreatedAt())
                .build();
    }

    private UserAdminLiteResponse toUserDetails(AdminUserDetailLiteProjection user) {
        return UserAdminLiteResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(assetUrlFactory.staticAssetUrl(user.getAvatarAssetId()))
                .dateJoined(user.getCreatedAt())
                .build();
    }
}
