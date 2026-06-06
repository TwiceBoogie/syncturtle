package com.syncturtle.services.instance.support.fixture;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.core.actor.PrincipalType;

public final class UserEventFixtures {

    private UserEventFixtures() {
    }

    public static UserEvent.UserEventBuilder activeUserEvent() {
        return UserEvent.builder()
                .eventId("evt-user-1")
                .occurredAt(Instant.parse("2026-05-22T12:00:00Z"))
                .type(UserEvent.Type.USER_CREATED)
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .username("luna")
                .email("lunasnow@marvel.com")
                .displayName("lunasnow123")
                .firstName("luna")
                .lastName("snow")
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .avatarAssetId(UUID.fromString("00000000-0000-0000-0000-0000000000101"))
                .coverImageAssetId(UUID.fromString("00000000-0000-0000-0000-0000000000102"))
                .active(true)
                .emailVerified(true)
                .passwordAutoset(false)
                .userTimezone("America/Chicago")
                .principalType(PrincipalType.HUMAN)
                .version(1L)
                .authVersion(1L);
    }

    public static UserEvent deletedUserEvent() {
        return activeUserEvent()
                .type(UserEvent.Type.USER_SOFT_DELETE)
                .active(false)
                .version(2L)
                .occurredAt(Instant.parse("2026-05-22T13:00:00Z"))
                .build();
    }

}
