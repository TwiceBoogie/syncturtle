package com.syncturtle.common.core.events;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    public enum Type {
        USER_CREATED, USER_UPDATED, USER_SOFT_DELETE
    }

    private String eventId;
    private Instant occurredAt;

    private Type type;

    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String firstName;
    private String lastName;
    private Instant dateJoined;
    private UUID avatarAssetId;
    private UUID coverImageAssetId;
    private boolean active;
    private boolean emailVerified;
    private boolean passwordAutoset;
    private String userTimezone;
    private boolean bot;
    private Long version;

    private Long authVersion;
}
