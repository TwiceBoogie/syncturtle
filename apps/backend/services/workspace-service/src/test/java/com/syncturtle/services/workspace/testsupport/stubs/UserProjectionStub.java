package com.syncturtle.services.workspace.testsupport.stubs;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.core.actor.PrincipalType;
import com.syncturtle.services.workspace.repository.projection.UserProjection;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class UserProjectionStub implements UserProjection {
    private final UUID id;
    private final String username;
    private final String email;
    private final String displayName;
    private final String firstName;
    private final String lastName;
    private final Instant createdAt;
    private final UUID avatarAssetId;
    private final UUID coverImageAssetId;
    private final boolean active;
    private final boolean emailVerified;
    private final boolean passwordAutoset;
    private final String userTimezone;
    private final PrincipalType principalType;
    private final boolean bot;
}
