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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.exception.RefreshSessionLifecycleException;
import com.syncturtle.services.user.model.InstanceLite;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceLiteRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionReceipt;
import com.syncturtle.services.user.service.collaborator.session.IssuedRefreshTokenReceipt;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionFamilyStore;
import com.syncturtle.services.user.service.collaborator.token.IssuedAccessTokenReceipt;
import com.syncturtle.services.user.service.param.AuthenticatedSessionIssueParam;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class RefreshSessionServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTANCE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String SESSION_ID = "33333333-3333-3333-3333-333333333333";
    private static final String PRESENTED = SESSION_ID + ".presented";
    private static final Instant NOW = Instant.parse("2026-08-19T10:00:00Z");

    @Mock
    private RefreshSessionFamilyStore familyStore;
    @Mock
    private InstanceAuthorizationResolver authorizationResolver;
    @Mock
    private AuthenticatedSessionIssuer sessionIssuer;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InstanceLiteRepository instanceRepository;
    @Mock
    private RequestClientContext clientContext;
    private RefreshSessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RefreshSessionServiceImpl(
                familyStore,
                authorizationResolver,
                sessionIssuer,
                userRepository,
                instanceRepository,
                clientContext);
    }

    @Nested
    class RefreshSession {

        @Test
        void rotatesThroughAtomicLifecycleAfterDurableAndAuthorityChecks() {
            // arrange
            RefreshSessionFamilyRecord family = family(List.of("USER"), null);
            User user = user(7L, true);
            InstanceLite instance = instance(true);
            InstanceAuthorizationSnapshot authorization = InstanceAuthorizationSnapshot.member();
            when(familyStore.extractSessionId(PRESENTED)).thenReturn(SESSION_ID);
            when(familyStore.requireFamily(SESSION_ID)).thenReturn(family);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.of(instance));
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID)).thenReturn(authorization);
            when(clientContext.getClientIp()).thenReturn("192.0.2.10");
            when(clientContext.getUserAgent()).thenReturn("Test browser");
            when(sessionIssuer.rotateSession(
                    any(AuthenticatedSessionIssueParam.class),
                    any(String.class),
                    any(InstanceAuthorizationSnapshot.class)))
                    .thenReturn(receipt());

            // act
            IssueTokenResponse result = service.refreshSession(PRESENTED);

            // assert
            assertThat(result.getRefreshToken()).isEqualTo(SESSION_ID + ".successor");
            verify(sessionIssuer).rotateSession(
                    any(AuthenticatedSessionIssueParam.class),
                    org.mockito.ArgumentMatchers.eq(PRESENTED),
                    org.mockito.ArgumentMatchers.same(authorization));
            verify(familyStore, never()).revokeOne(any(), any());
        }

        @Test
        void revokesFamilyWhenDurableUserVersionNoLongerMatches() {
            // arrange
            User user = userWithoutId(8L, true);
            when(familyStore.extractSessionId(PRESENTED)).thenReturn(SESSION_ID);
            when(familyStore.requireFamily(SESSION_ID)).thenReturn(family(List.of("USER"), null));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            // act
            AuthException failure = catchThrowableOfType(
                    AuthException.class,
                    () -> service.refreshSession(PRESENTED));

            // assert
            assertThat(failure.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED);
            verify(familyStore).revokeOne(USER_ID.toString(), SESSION_ID);
            verify(sessionIssuer, never()).rotateSession(any(), any(), any());
        }

        @Test
        void revokesAdministratorFamilyWhenInstanceAuthorityVersionChanges() {
            // arrange
            User user = user(7L, true);
            InstanceLite instance = instance(true);
            when(familyStore.extractSessionId(PRESENTED)).thenReturn(SESSION_ID);
            when(familyStore.requireFamily(SESSION_ID))
                    .thenReturn(family(List.of("INSTANCE_ADMIN", "USER"), 9L));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.of(instance));
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID))
                    .thenReturn(InstanceAuthorizationSnapshot.instanceAdmin(10L));

            // act
            AuthException failure = catchThrowableOfType(
                    AuthException.class,
                    () -> service.refreshSession(PRESENTED));

            // assert
            assertThat(failure.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED);
            verify(familyStore).revokeOne(USER_ID.toString(), SESSION_ID);
        }

        @Test
        void mapsReplayAndUnsupportedStateToGenericAuthenticationFailure() {
            // arrange
            when(familyStore.extractSessionId(PRESENTED)).thenReturn(SESSION_ID);
            when(familyStore.requireFamily(SESSION_ID)).thenThrow(
                    new RefreshSessionLifecycleException(
                            RefreshSessionLifecycleException.Reason.UNSUPPORTED_RECORD_VERSION,
                            "unsupported"));

            // act
            AuthException failure = catchThrowableOfType(
                    AuthException.class,
                    () -> service.refreshSession(PRESENTED));

            // assert
            assertThat(failure.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        @Test
        void mapsRedisFailureToServiceUnavailableWithoutCredentialFallback() {
            // arrange
            when(familyStore.extractSessionId(PRESENTED)).thenReturn(SESSION_ID);
            when(familyStore.requireFamily(SESSION_ID)).thenThrow(
                    new RefreshSessionLifecycleException(
                            RefreshSessionLifecycleException.Reason.REDIS_FAILURE,
                            "redis unavailable"));

            // act
            RemoteServiceException failure = catchThrowableOfType(
                    RemoteServiceException.class,
                    () -> service.refreshSession(PRESENTED));

            // assert
            assertThat(failure.getErrorCode().getHttpStatusCode()).isEqualTo(503);
            verify(sessionIssuer, never()).rotateSession(any(), any(), any());
        }

        @Test
        void mapsReplayReturnedByAtomicRotationToGenericAuthenticationFailure() {
            // arrange
            RefreshSessionFamilyRecord family = family(List.of("USER"), null);
            User user = user(7L, true);
            InstanceLite instance = instance(true);
            when(familyStore.extractSessionId(PRESENTED)).thenReturn(SESSION_ID);
            when(familyStore.requireFamily(SESSION_ID)).thenReturn(family);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.of(instance));
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID))
                    .thenReturn(InstanceAuthorizationSnapshot.member());
            when(clientContext.getClientIp()).thenReturn("192.0.2.10");
            when(clientContext.getUserAgent()).thenReturn("Test browser");
            when(sessionIssuer.rotateSession(any(), any(), any())).thenThrow(
                    new RefreshSessionLifecycleException(
                            RefreshSessionLifecycleException.Reason.REPLAY_REVOKED,
                            "replay"));

            // act
            AuthException failure = catchThrowableOfType(
                    AuthException.class,
                    () -> service.refreshSession(PRESENTED));

            // assert
            assertThat(failure.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED);
        }
    }

    private RefreshSessionFamilyRecord family(List<String> roles, Long adminVersion) {
        Instant createdAt = NOW.minusSeconds(3600);
        return RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId(USER_ID.toString())
                .instanceId(INSTANCE_ID.toString())
                .roles(roles)
                .authVersion(7L)
                .adminSessionVersion(adminVersion)
                .currentRefreshTokenHash("a".repeat(64))
                .rotationCounter(2L)
                .createdAt(createdAt)
                .lastUsedAt(NOW)
                .idleExpiresAt(NOW.plus(RefreshSessionFamilyRecord.IDLE_LIFETIME))
                .absoluteExpiresAt(createdAt.plus(RefreshSessionFamilyRecord.ABSOLUTE_LIFETIME))
                .deviceLabel("Test browser")
                .clientBindingHash("b".repeat(64))
                .build();
    }

    private User user(long authVersion, boolean loginAllowed) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getAuthVersion()).thenReturn(authVersion);
        when(user.isLoginAllowed()).thenReturn(loginAllowed);
        return user;
    }

    private User userWithoutId(long authVersion, boolean loginAllowed) {
        User user = mock(User.class);
        when(user.getAuthVersion()).thenReturn(authVersion);
        when(user.isLoginAllowed()).thenReturn(loginAllowed);
        return user;
    }

    private InstanceLite instance(boolean setupDone) {
        InstanceLite instance = mock(InstanceLite.class);
        when(instance.getId()).thenReturn(INSTANCE_ID);
        when(instance.isSetupDone()).thenReturn(setupDone);
        return instance;
    }

    private AuthenticatedSessionReceipt receipt() {
        return AuthenticatedSessionReceipt.builder()
                .accessToken(IssuedAccessTokenReceipt.builder()
                        .token("access-token")
                        .issuedAt(NOW)
                        .expiresAt(NOW.plusSeconds(900))
                        .build())
                .refreshToken(IssuedRefreshTokenReceipt.builder()
                        .sessionId(SESSION_ID)
                        .token(SESSION_ID + ".successor")
                        .issuedAt(NOW)
                        .expiresAt(NOW.plusSeconds(3600))
                        .build())
                .build();
    }
}
