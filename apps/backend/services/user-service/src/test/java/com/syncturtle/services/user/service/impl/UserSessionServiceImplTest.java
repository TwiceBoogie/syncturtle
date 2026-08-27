package com.syncturtle.services.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.dto.response.UserSessionInventoryResponse;
import com.syncturtle.services.user.dto.response.UserSessionResponse;
import com.syncturtle.services.user.dto.response.UserSessionRevocationResponse;
import com.syncturtle.services.user.exception.RefreshSessionLifecycleException;
import com.syncturtle.services.user.service.UserSessionService;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionFamilySnapshot;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionFamilyStore;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CURRENT_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final UUID FIRST_TIE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SECOND_TIE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant NOW = Instant.parse("2026-08-25T15:00:00Z");

    @Mock
    private RefreshSessionFamilyStore familyStore;

    private UserSessionService service;

    @BeforeEach
    void setup() {
        service = new UserSessionServiceImpl(familyStore, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    class ListSessions {

        @Test
        void mapsOnlyPublicFieldsAndOrdersByActivityThenCanonicalSessionId() {
            // arrange
            RefreshSessionFamilySnapshot current = snapshot(
                    CURRENT_ID,
                    NOW.minusSeconds(30),
                    List.of("USER"),
                    null,
                    "raw user agent must not escape");
            RefreshSessionFamilySnapshot secondTie = snapshot(
                    SECOND_TIE_ID,
                    NOW,
                    List.of("USER"),
                    null,
                    "another user agent");
            RefreshSessionFamilySnapshot firstTie = snapshot(
                    FIRST_TIE_ID,
                    NOW,
                    List.of("INSTANCE_ADMIN", "USER"),
                    9L,
                    "administrator user agent");
            // conditions
            when(familyStore.inventory(USER_ID.toString(), CURRENT_ID.toString(), NOW))
                    .thenReturn(List.of(current, secondTie, firstTie));
            // act
            UserSessionInventoryResponse result = service.listSessions(USER_ID, CURRENT_ID);
            // assert
            assertThat(result.getSessions())
                    .extracting(UserSessionResponse::getSessionId)
                    .containsExactly(FIRST_TIE_ID, SECOND_TIE_ID, CURRENT_ID);
            assertThat(result.getSessions().get(0).isAdministrator()).isTrue();
            assertThat(result.getSessions().get(0).isCurrent()).isFalse();
            assertThat(result.getSessions().get(2).isCurrent()).isTrue();
            assertThat(result.getSessions().get(2).getLastUsedAt()).isEqualTo(NOW.minusSeconds(30));
            // verify
        }

        @Test
        void mapsMissingOrExpiredCurrentToEstablishedAuthenticationFailure() {
            // arrange
            // conditions
            when(familyStore.inventory(USER_ID.toString(), CURRENT_ID.toString(), NOW)).thenThrow(
                    new RefreshSessionLifecycleException(
                            RefreshSessionLifecycleException.Reason.MISSING_FAMILY,
                            "missing"));
            // act
            AuthException failure = catchThrowableOfType(
                    AuthException.class,
                    () -> service.listSessions(USER_ID, CURRENT_ID));
            // assert
            assertThat(failure.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED);
            // verify
        }

        @Test
        void mapsMalformedOrUnavailableInventoryToGenericServiceUnavailable() {
            // arrange
            // conditions
            when(familyStore.inventory(USER_ID.toString(), CURRENT_ID.toString(), NOW)).thenThrow(
                    new RefreshSessionLifecycleException(
                            RefreshSessionLifecycleException.Reason.REDIS_FAILURE,
                            "internal detail"));
            // act
            RemoteServiceException failure = catchThrowableOfType(
                    RemoteServiceException.class,
                    () -> service.listSessions(USER_ID, CURRENT_ID));
            // assert
            assertThat(failure.getErrorCode().getHttpStatusCode()).isEqualTo(503);
        }
    }

    @Nested
    class RevokeSession {

        @Test
        void returnsTrueOnlyWhenTargetEqualsTrustedCurrentSession() {
            // arrange
            // conditions
            // act
            UserSessionRevocationResponse current = service.revokeSession(USER_ID, CURRENT_ID, CURRENT_ID);
            UserSessionRevocationResponse other = service.revokeSession(USER_ID, CURRENT_ID, FIRST_TIE_ID);
            // assert
            assertThat(current.isCurrentSessionRevoked()).isTrue();
            assertThat(other.isCurrentSessionRevoked()).isFalse();
            // verify
            verify(familyStore).revokeOne(USER_ID.toString(), CURRENT_ID.toString());
            verify(familyStore).revokeOne(USER_ID.toString(), FIRST_TIE_ID.toString());
        }

        @Test
        void doesNotReturnSuccessWhenRedisFails() {
            // arrange + arrange
            doThrow(new RefreshSessionLifecycleException(
                    RefreshSessionLifecycleException.Reason.REDIS_FAILURE,
                    "redis unavailable"))
                    .when(familyStore)
                    .revokeOne(USER_ID.toString(), CURRENT_ID.toString());
            // act
            RemoteServiceException failure = catchThrowableOfType(
                    RemoteServiceException.class,
                    () -> service.revokeSession(USER_ID, CURRENT_ID, CURRENT_ID));
            // assert
            assertThat(failure.getErrorCode().getHttpStatusCode()).isEqualTo(503);
            // verify
        }
    }

    @Nested
    class RevokeOtherSessions {

        @Test
        void delegatesOneStoreOperationAndPreservesCurrent() {
            // arrange
            // conditions
            // act
            UserSessionRevocationResponse result = service.revokeOtherSessions(USER_ID, CURRENT_ID);
            // assert
            assertThat(result.isCurrentSessionRevoked()).isFalse();
            // verify
            verify(familyStore).revokeOthers(USER_ID.toString(), CURRENT_ID.toString());
            verify(familyStore, never()).revokeAll(USER_ID.toString(), CURRENT_ID.toString());
        }
    }

    @Nested
    class RevokeAllSessions {

        @Test
        void suppliesExplicitCurrentAndReturnsCurrentRevoked() {
            // arrange
            // conditions
            // act
            UserSessionRevocationResponse result = service.revokeAllSessions(USER_ID, CURRENT_ID);
            // assert
            assertThat(result.isCurrentSessionRevoked()).isTrue();
            // verify
            verify(familyStore).revokeAll(USER_ID.toString(), CURRENT_ID.toString());
            verify(familyStore, never()).revokeAll(USER_ID.toString());
        }
    }

    private static RefreshSessionFamilySnapshot snapshot(
            UUID sessionId,
            Instant lastUsedAt,
            List<String> roles,
            Long adminSessionVersion,
            String deviceLabel) {
        Instant createdAt = NOW.minusSeconds(3600);
        Instant absoluteExpiresAt = createdAt.plus(RefreshSessionFamilyRecord.ABSOLUTE_LIFETIME);
        Instant idleExpiresAt = lastUsedAt.plus(RefreshSessionFamilyRecord.IDLE_LIFETIME);
        if (idleExpiresAt.isAfter(absoluteExpiresAt)) {
            idleExpiresAt = absoluteExpiresAt;
        }

        RefreshSessionFamilyRecord family = RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId(USER_ID.toString())
                .instanceId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .roles(roles)
                .authVersion(7L)
                .adminSessionVersion(adminSessionVersion)
                .currentRefreshTokenHash("a".repeat(64))
                .rotationCounter(2L)
                .createdAt(createdAt)
                .lastUsedAt(lastUsedAt)
                .idleExpiresAt(idleExpiresAt)
                .absoluteExpiresAt(absoluteExpiresAt)
                .deviceLabel(deviceLabel)
                .clientBindingHash("b".repeat(64))
                .build();
        return new RefreshSessionFamilySnapshot(sessionId, family);
    }

}
