package com.syncturtle.services.instance.controllers.mappers;

import org.springframework.stereotype.Component;

import com.syncturtle.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.services.instance.dto.response.UserAdminLiteResponse;
import com.syncturtle.services.instance.models.InstanceAdmin;
import com.syncturtle.services.instance.models.User;

@Component
public class InstanceAdminApiMapper {

    public InstanceAdminResponse toResponse(InstanceAdmin instanceAdmin) {
        return InstanceAdminResponse.builder()
                .id(instanceAdmin.getId())
                .instance(instanceAdmin.getInstance() == null ? null : instanceAdmin.getInstance().getId())
                .user(instanceAdmin.getUserId())
                .role(instanceAdmin.getRole())
                .createdAt(instanceAdmin.getCreatedAt())
                .updatedAt(instanceAdmin.getUpdatedAt())
                .userDetail(toUserAdminLiteResponse(instanceAdmin.getUser()))
                .createdBy(instanceAdmin.getCreatedById())
                .updatedBy(instanceAdmin.getUpdatedById())
                .build();
    }

    public UserAdminLiteResponse toUserAdminLiteResponse(User user) {
        return UserAdminLiteResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarAssetId().toString())
                .joiningDate(user.getDateJoined())
                .build();
    }

}
