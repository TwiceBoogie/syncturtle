package com.syncturtle.platform.services.workspace.testsupport.stubs;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.platform.services.workspace.repositories.projections.UserProjection;

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
    private final Instant dateJoined;
    private final UUID avatarAssetId;
    private final UUID coverImageAssetId;
    private final boolean active;
    private final boolean emailVerified;
    private final boolean passwordAutoset;
    private final String timezone;
    private final boolean bot;
}
