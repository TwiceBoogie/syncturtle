package com.syncturtle.services.workspace.messaging.kafka.mapper;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.services.workspace.model.param.UserReplicaParam;

@Component
public final class UserEventMapper {

    public UserReplicaParam toParam(UserEvent event) {
        Assert.notNull(event, "user event is required");

        return UserReplicaParam.builder()
                .id(event.getId())
                .sourceVersion(event.getVersion())
                .username(event.getUsername())
                .email(event.getEmail())
                .displayName(event.getDisplayName())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .avatarAssetId(event.getAvatarAssetId())
                .coverImageAssetId(event.getCoverImageAssetId())
                .createdAt(event.getCreatedAt())
                .active(resolveActive(event))
                .emailVerified(event.isEmailVerified())
                .passwordAutoset(event.isPasswordAutoset())
                .userTimezone(event.getUserTimezone())
                .principalType(event.getPrincipalType())
                .authVersion(event.getAuthVersion())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getOccurredAt())
                .deleteEvent(event.isDeleteEvent())
                .build();
    }

    private static boolean resolveActive(UserEvent event) {
        if (event.isDeleteEvent()) {
            return false;
        }

        return event.isActive();
    }

}
