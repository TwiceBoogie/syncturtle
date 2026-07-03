package com.syncturtle.services.user.messaging.kafka.factory;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.services.user.model.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class UserEventFactory {

    private final Clock clock;

    public UserEvent created(User user) {
        return from(user, UserEvent.Type.USER_CREATED);
    }

    public UserEvent updated(User user) {
        return from(user, UserEvent.Type.USER_UPDATED);
    }

    private UserEvent from(User user, UserEvent.Type type) {
        Assert.notNull(user, "user is required");
        Assert.notNull(type, "user event type is required");

        return UserEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(Instant.now(clock))
                .type(type)
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarAssetId(user.getAvatarAssetId())
                .coverImageAssetId(user.getCoverImageAssetId())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .passwordAutoset(user.isPasswordAutoset())
                .userTimezone(user.getUserTimezone())
                .principalType(user.getPrincipalType())
                .authVersion(user.getAuthVersion())
                .updatedById(user.getUpdatedById())
                .createdById(user.getCreatedById())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .version(user.getVersion())
                .build();
    }

}
