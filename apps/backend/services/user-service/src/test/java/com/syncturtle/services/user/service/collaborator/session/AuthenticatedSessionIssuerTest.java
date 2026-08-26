package com.syncturtle.services.user.service.collaborator.session;

import static com.syncturtle.services.user.support.fixture.RefreshSessionPropertyFixtures.clientBindingProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.exception.RefreshSessionLifecycleException;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.collaborator.token.AccessTokenIssuer;
import com.syncturtle.services.user.service.collaborator.token.IssuedAccessTokenReceipt;
import com.syncturtle.services.user.service.param.AccessTokenIssueParam;
import com.syncturtle.services.user.service.param.AuthenticatedSessionIssueParam;
import com.syncturtle.services.user.service.param.AuthorizedSessionIssueParam;
import com.syncturtle.services.user.service.param.RefreshSessionFamilyCreateParam;
import com.syncturtle.services.user.service.param.RefreshSessionFamilyRotateParam;
import com.syncturtle.services.user.type.RefreshSessionLifecycleOutcome;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class AuthenticatedSessionIssuerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTANCE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String SESSION_ID = "33333333-3333-3333-3333-333333333333";
    private static final Instant CREATED_AT = Instant.parse("2026-08-19T10:00:00Z");

    @Mock
    private RefreshSessionFamilyStore familyStore;
    @Mock
    private AccessTokenIssuer accessTokenIssuer;
    @Mock
    private InstanceAuthorizationResolver authorizationResolver;
    @Captor
    private ArgumentCaptor<RefreshSessionFamilyCreateParam> createParamCaptor;
    @Captor
    private ArgumentCaptor<AccessTokenIssueParam> accessTokenParamCaptor;

    private AuthenticatedSessionIssuer issuer;

    @BeforeEach
    void setUp() {
        RefreshSessionClientFingerprintFactory fingerprintFactory = new RefreshSessionClientFingerprintFactory(
                clientBindingProperties());
        issuer = new AuthenticatedSessionIssuer(
                familyStore,
                fingerprintFactory,
                accessTokenIssuer,
                authorizationResolver);
    }

    @Nested
    class IssueNewSession {

        @Test
        void createsV2AndSignsOnlyFromTheAuthoritativeFamilyResult() {
            // arrange
            User user = user();
            RefreshSessionLifecycleResult lifecycle = lifecycleResult(
                    RefreshSessionLifecycleOutcome.CREATED,
                    family(List.of("INSTANCE_ADMIN", "USER"), 7L, 9L, 0L));
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID))
                    .thenReturn(InstanceAuthorizationSnapshot.instanceAdmin(9L));
            when(familyStore.create(any(RefreshSessionFamilyCreateParam.class))).thenReturn(lifecycle);
            when(accessTokenIssuer.issueAccessToken(any(AccessTokenIssueParam.class))).thenReturn(accessReceipt());

            // act
            AuthenticatedSessionReceipt result = issuer.issueNewSession(issueParam(user));

            // assert
            verify(familyStore).create(createParamCaptor.capture());
            verify(accessTokenIssuer).issueAccessToken(accessTokenParamCaptor.capture());
            assertThat(createParamCaptor.getValue().getRoles()).containsExactly("INSTANCE_ADMIN", "USER");
            assertThat(createParamCaptor.getValue().getClientBindingHash()).hasSize(64);
            assertThat(accessTokenParamCaptor.getValue().getSessionId()).isEqualTo(SESSION_ID);
            assertThat(accessTokenParamCaptor.getValue().getUserAuthVersion()).isEqualTo(7L);
            assertThat(accessTokenParamCaptor.getValue().getAdminSessionVersion()).isEqualTo(9L);
            assertThat(result.getRefreshToken().getSessionId()).isEqualTo(SESSION_ID);
            assertThat(result.getRefreshToken().getExpiresAt()).isEqualTo(lifecycle.getFamily().getIdleExpiresAt());
        }

        @Test
        void revokesNewFamilyWhenAccessSigningFails() {
            // arrange
            User user = user();
            RefreshSessionLifecycleResult lifecycle = lifecycleResult(
                    RefreshSessionLifecycleOutcome.CREATED,
                    family(List.of("USER"), 7L, null, 0L));
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID))
                    .thenReturn(InstanceAuthorizationSnapshot.member());
            when(familyStore.create(any(RefreshSessionFamilyCreateParam.class))).thenReturn(lifecycle);
            IllegalStateException signingFailure = new IllegalStateException("signing failed");
            when(accessTokenIssuer.issueAccessToken(any(AccessTokenIssueParam.class))).thenThrow(signingFailure);

            // act
            IllegalStateException failure = catchThrowableOfType(
                    IllegalStateException.class,
                    () -> issuer.issueNewSession(issueParam(user)));

            // assert
            assertThat(failure).isSameAs(signingFailure);
            verify(familyStore).revokeOne(USER_ID.toString(), SESSION_ID);
        }

        @Test
        void failsClosedWhenAtomicCreationCannotReachRedis() {
            // arrange
            User user = user();
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID))
                    .thenReturn(InstanceAuthorizationSnapshot.member());
            when(familyStore.create(any(RefreshSessionFamilyCreateParam.class))).thenThrow(
                    new RefreshSessionLifecycleException(
                            RefreshSessionLifecycleException.Reason.REDIS_FAILURE,
                            "redis unavailable"));

            // act
            RemoteServiceException failure = catchThrowableOfType(
                    RemoteServiceException.class,
                    () -> issuer.issueNewSession(issueParam(user)));

            // assert
            assertThat(failure.getHttpStatusCode()).isEqualTo(503);
            verify(accessTokenIssuer, never()).issueAccessToken(any(AccessTokenIssueParam.class));
        }

        @Test
        void rejectsMissingUserBeforeReadingPrincipalFields() {
            // arrange
            AuthenticatedSessionIssueParam param = mock(AuthenticatedSessionIssueParam.class);

            // act
            IllegalArgumentException failure = catchThrowableOfType(
                    IllegalArgumentException.class,
                    () -> issuer.issueNewSession(param));

            // assert
            assertThat(failure).hasMessage("user is required");
            verify(familyStore, never()).create(any());
        }
    }

    @Nested
    class IssueAuthorizedSession {

        @Test
        void preservesTheExistingAdministratorRoleContract() {
            // arrange
            RefreshSessionLifecycleResult lifecycle = lifecycleResult(
                    RefreshSessionLifecycleOutcome.CREATED,
                    family(List.of("INSTANCE_ADMIN"), 7L, 9L, 0L));
            when(familyStore.create(any(RefreshSessionFamilyCreateParam.class))).thenReturn(lifecycle);
            when(accessTokenIssuer.issueAccessToken(any(AccessTokenIssueParam.class))).thenReturn(accessReceipt());
            AuthorizedSessionIssueParam param = AuthorizedSessionIssueParam.builder()
                    .userId(USER_ID.toString())
                    .instanceId(INSTANCE_ID.toString())
                    .roles(List.of("INSTANCE_ADMIN"))
                    .authVersion(7L)
                    .adminSessionVersion(9L)
                    .ipAddress("192.0.2.10")
                    .userAgent("Admin browser")
                    .build();

            // act
            AuthenticatedSessionReceipt result = issuer.issueAuthorizedSession(param);

            // assert
            assertThat(result.getRefreshToken().getSessionId()).isEqualTo(SESSION_ID);
            verify(authorizationResolver, never()).resolve(any(), any());
        }
    }

    @Nested
    class RotateSession {

        @Test
        void keepsSidAndVersionsBoundToTheRotatedFamily() {
            // arrange
            User user = user();
            RefreshSessionLifecycleResult lifecycle = lifecycleResult(
                    RefreshSessionLifecycleOutcome.ROTATED,
                    family(List.of("USER"), 7L, null, 4L));
            when(familyStore.rotate(any(RefreshSessionFamilyRotateParam.class))).thenReturn(lifecycle);
            when(accessTokenIssuer.issueAccessToken(any(AccessTokenIssueParam.class))).thenReturn(accessReceipt());

            // act
            AuthenticatedSessionReceipt result = issuer.rotateSession(
                    issueParam(user),
                    SESSION_ID + ".previous",
                    InstanceAuthorizationSnapshot.member());

            // assert
            ArgumentCaptor<AccessTokenIssueParam> access = ArgumentCaptor.forClass(AccessTokenIssueParam.class);
            verify(accessTokenIssuer).issueAccessToken(access.capture());
            assertThat(access.getValue().getSessionId()).isEqualTo(SESSION_ID);
            assertThat(access.getValue().getUserAuthVersion()).isEqualTo(7L);
            assertThat(result.getRefreshToken().getSessionId()).isEqualTo(SESSION_ID);
        }

        @Test
        void leavesRotatedFamilyRecoverableWhenAccessSigningFails() {
            // arrange
            User user = user();
            RefreshSessionLifecycleResult lifecycle = lifecycleResult(
                    RefreshSessionLifecycleOutcome.ROTATED,
                    family(List.of("USER"), 7L, null, 4L));
            when(familyStore.rotate(any(RefreshSessionFamilyRotateParam.class))).thenReturn(lifecycle);
            when(accessTokenIssuer.issueAccessToken(any(AccessTokenIssueParam.class)))
                    .thenThrow(new IllegalStateException("signing failed"));

            // act
            IllegalStateException failure = catchThrowableOfType(
                    IllegalStateException.class,
                    () -> issuer.rotateSession(
                            issueParam(user),
                            SESSION_ID + ".previous",
                            InstanceAuthorizationSnapshot.member()));

            // assert
            assertThat(failure).hasMessage("signing failed");
            verify(familyStore, never()).revokeOne(any(), any());
        }

        @Test
        void mapsGraceRecoveryToTheSameAuthoritativeSuccessorWithoutAnotherTransition() {
            // arrange
            User user = user();
            RefreshSessionLifecycleResult lifecycle = lifecycleResult(
                    RefreshSessionLifecycleOutcome.GRACE_RECOVERED,
                    family(List.of("USER"), 7L, null, 4L));
            when(familyStore.rotate(any(RefreshSessionFamilyRotateParam.class))).thenReturn(lifecycle);
            when(accessTokenIssuer.issueAccessToken(any(AccessTokenIssueParam.class))).thenReturn(accessReceipt());

            // act
            AuthenticatedSessionReceipt result = issuer.rotateSession(
                    issueParam(user),
                    SESSION_ID + ".previous",
                    InstanceAuthorizationSnapshot.member());

            // assert
            assertThat(result.getRefreshToken().getToken()).isEqualTo(SESSION_ID + ".successor");
            assertThat(result.getRefreshToken().getSessionId()).isEqualTo(SESSION_ID);
            verify(familyStore).rotate(any(RefreshSessionFamilyRotateParam.class));
        }
    }

    private User user() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getAuthVersion()).thenReturn(7L);
        when(user.getEmail()).thenReturn("user@example.test");
        return user;
    }

    private AuthenticatedSessionIssueParam issueParam(User user) {
        return AuthenticatedSessionIssueParam.builder()
                .user(user)
                .instanceId(INSTANCE_ID)
                .ipAddress("192.0.2.10")
                .userAgent("Test browser")
                .build();
    }

    private RefreshSessionLifecycleResult lifecycleResult(
            RefreshSessionLifecycleOutcome outcome,
            RefreshSessionFamilyRecord family) {
        return new RefreshSessionLifecycleResult(
                outcome,
                SESSION_ID,
                SESSION_ID + ".successor",
                family);
    }

    private RefreshSessionFamilyRecord family(
            List<String> roles,
            long authVersion,
            Long adminSessionVersion,
            long rotationCounter) {
        Instant lastUsedAt = CREATED_AT.plusSeconds(rotationCounter * 60);
        return RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId(USER_ID.toString())
                .instanceId(INSTANCE_ID.toString())
                .roles(roles)
                .authVersion(authVersion)
                .adminSessionVersion(adminSessionVersion)
                .currentRefreshTokenHash("a".repeat(64))
                .rotationCounter(rotationCounter)
                .createdAt(CREATED_AT)
                .lastUsedAt(lastUsedAt)
                .idleExpiresAt(lastUsedAt.plus(RefreshSessionFamilyRecord.IDLE_LIFETIME))
                .absoluteExpiresAt(CREATED_AT.plus(RefreshSessionFamilyRecord.ABSOLUTE_LIFETIME))
                .deviceLabel("Test browser")
                .clientBindingHash("b".repeat(64))
                .build();
    }

    private IssuedAccessTokenReceipt accessReceipt() {
        return IssuedAccessTokenReceipt.builder()
                .token("access-token")
                .issuedAt(CREATED_AT)
                .expiresAt(CREATED_AT.plusSeconds(900))
                .build();
    }
}
