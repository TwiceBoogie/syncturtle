package com.syncturtle.services.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.exception.RefreshSessionException;
import com.syncturtle.services.user.model.InstanceLite;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceLiteRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionSnapshot;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionStore;

@ExtendWith(MockitoExtension.class)
class RefreshSessionServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTANCE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String SID = "33333333-3333-3333-3333-333333333333";
    private static final String TOKEN = SID + "." + "a".repeat(64);
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Mock RefreshSessionStore sessions;
    @Mock InstanceAuthorizationResolver authorizationResolver;
    @Mock AuthenticatedSessionIssuer issuer;
    @Mock UserRepository users;
    @Mock InstanceLiteRepository instances;
    private RefreshSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RefreshSessionServiceImpl(sessions, authorizationResolver, issuer, users, instances);
    }

    @Test
    void stalePresentedCredentialDoesNotReachLiveLookupOrRotation() {
        when(sessions.requirePresented(TOKEN))
                .thenThrow(new RefreshSessionException(RefreshSessionException.Reason.INVALID, "stale"));

        AuthException failure = catchThrowableOfType(AuthException.class,
                () -> service.refreshSession(TOKEN));

        assertThat(failure.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED);
        verify(sessions, never()).rotate(any(), any());
    }

    @Test
    void redisOutageDoesNotFallBackToJwtOnlyRefresh() {
        when(sessions.requirePresented(TOKEN))
                .thenThrow(new RefreshSessionException(RefreshSessionException.Reason.UNAVAILABLE, "redis"));

        RemoteServiceException failure = catchThrowableOfType(RemoteServiceException.class,
                () -> service.refreshSession(TOKEN));

        assertThat(failure.getErrorCode().getHttpStatusCode()).isEqualTo(503);
        verify(sessions, never()).rotate(any(), any());
    }

    @Test
    void changedDurableUserVersionRejectsBeforeRotation() {
        when(sessions.requirePresented(TOKEN)).thenReturn(snapshot(4L, null, List.of("USER")));
        User user = mock(User.class);
        when(user.isLoginAllowed()).thenReturn(true);
        when(user.getAuthVersion()).thenReturn(5L);
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));

        AuthException failure = catchThrowableOfType(AuthException.class,
                () -> service.refreshSession(TOKEN));

        assertThat(failure.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED);
        verify(sessions, never()).rotate(any(), any());
    }

    @Test
    void removedAdministratorIsRejectedByLiveAuthorization() {
        when(sessions.requirePresented(TOKEN))
                .thenReturn(snapshot(4L, 8L, List.of("INSTANCE_ADMIN", "USER")));
        User user = mock(User.class);
        when(user.isLoginAllowed()).thenReturn(true);
        when(user.getAuthVersion()).thenReturn(4L);
        when(user.getId()).thenReturn(USER_ID);
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        InstanceLite instance = mock(InstanceLite.class);
        when(instance.isSetupDone()).thenReturn(true);
        when(instance.getId()).thenReturn(INSTANCE_ID);
        when(instances.findById(INSTANCE_ID)).thenReturn(Optional.of(instance));
        when(authorizationResolver.resolve(USER_ID, INSTANCE_ID))
                .thenReturn(InstanceAuthorizationSnapshot.member());

        AuthException failure = catchThrowableOfType(AuthException.class,
                () -> service.refreshSession(TOKEN));

        assertThat(failure.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED);
        verify(sessions, never()).rotate(any(), any());
    }

    private RefreshSessionSnapshot snapshot(long authVersion, Long adminVersion, List<String> roles) {
        RefreshSessionRecord record = RefreshSessionRecord.builder()
                .recordVersion(1).userId(USER_ID.toString()).instanceId(INSTANCE_ID.toString())
                .roles(roles).authVersion(authVersion).adminSessionVersion(adminVersion)
                .currentRefreshTokenHash("a".repeat(64))
                .createdAt(NOW).idleExpiresAt(NOW.plus(RefreshSessionRecord.IDLE_LIFETIME))
                .absoluteExpiresAt(NOW.plus(RefreshSessionRecord.ABSOLUTE_LIFETIME)).build();
        return new RefreshSessionSnapshot(SID, "exact-json", record);
    }
}
