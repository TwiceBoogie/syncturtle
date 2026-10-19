package com.syncturtle.services.user.service.collaborator.session;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.exception.RefreshSessionException;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.service.collaborator.token.AccessTokenIssuer;
import com.syncturtle.services.user.service.collaborator.token.IssuedAccessTokenReceipt;
import com.syncturtle.services.user.service.param.AccessTokenIssueParam;
import com.syncturtle.services.user.service.param.AuthorizedSessionIssueParam;
import com.syncturtle.services.user.service.param.AuthenticatedSessionIssueParam;

@ExtendWith(MockitoExtension.class)
class AuthenticatedSessionIssuerTest {

    private static final String USER = "11111111-1111-1111-1111-111111111111";
    private static final String INSTANCE = "22222222-2222-2222-2222-222222222222";
    private static final String SID = "33333333-3333-3333-3333-333333333333";
    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    @Mock RefreshSessionStore store;
    @Mock AccessTokenIssuer access;
    @Mock InstanceAuthorizationResolver authorization;
    private AuthenticatedSessionIssuer issuer;

    @BeforeEach
    void setUp() {
        issuer = new AuthenticatedSessionIssuer(store, access, authorization);
    }

    @Test
    void issuesAccessClaimsFromStoredSessionReceipt() {
        RefreshSessionReceipt refresh = receipt();
        when(store.create(any(AuthorizedSessionIssueParam.class))).thenReturn(refresh);
        when(access.issueAccessToken(any(AccessTokenIssueParam.class))).thenReturn(
                IssuedAccessTokenReceipt.builder().token("jwt").issuedAt(NOW)
                        .expiresAt(NOW.plusSeconds(3600)).build());

        AuthenticatedSessionReceipt issued = issuer.issueAuthorizedSession(param());

        assertThat(issued.getRefreshToken().getSessionId()).isEqualTo(SID);
        assertThat(issued.getRefreshToken().getExpiresAt()).isEqualTo(refresh.getRecord().getIdleExpiresAt());
        verify(access).issueAccessToken(any(AccessTokenIssueParam.class));
    }

    @Test
    void newLoginUsesLiveAuthorizationAndCurrentUserVersion() {
        User user = mock(User.class);
        UUID userId = UUID.fromString(USER);
        UUID instanceId = UUID.fromString(INSTANCE);
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("user@syncturtle.test");
        when(user.getAuthVersion()).thenReturn(4L);
        when(authorization.resolve(userId, instanceId)).thenReturn(InstanceAuthorizationSnapshot.member());
        when(store.create(any(AuthorizedSessionIssueParam.class))).thenReturn(receipt());
        when(access.issueAccessToken(any(AccessTokenIssueParam.class))).thenReturn(
                IssuedAccessTokenReceipt.builder().token("jwt").issuedAt(NOW)
                        .expiresAt(NOW.plusSeconds(3600)).build());
        AuthenticatedSessionIssueParam login = AuthenticatedSessionIssueParam.builder()
                .user(user).instanceId(instanceId).ipAddress("127.0.0.1").userAgent("test").build();

        AuthenticatedSessionReceipt issued = issuer.issueNewSession(login);

        ArgumentCaptor<AuthorizedSessionIssueParam> captured =
                ArgumentCaptor.forClass(AuthorizedSessionIssueParam.class);
        verify(store).create(captured.capture());
        assertThat(captured.getValue().getUserId()).isEqualTo(USER);
        assertThat(captured.getValue().getInstanceId()).isEqualTo(INSTANCE);
        assertThat(captured.getValue().getAuthVersion()).isEqualTo(4L);
        assertThat(captured.getValue().getRoles()).containsExactly("USER");
        assertThat(issued.getRefreshToken().getSessionId()).isEqualTo(SID);
    }

    @Test
    void signingFailureRevokesNewSession() {
        when(store.create(any(AuthorizedSessionIssueParam.class))).thenReturn(receipt());
        when(access.issueAccessToken(any(AccessTokenIssueParam.class)))
                .thenThrow(new IllegalStateException("signing failed"));

        IllegalStateException failure = catchThrowableOfType(IllegalStateException.class,
                () -> issuer.issueAuthorizedSession(param()));

        assertThat(failure).hasMessage("signing failed");
        verify(store).revokeSession(SID);
    }

    @Test
    void redisOutagePreventsJwtIssuance() {
        when(store.create(any(AuthorizedSessionIssueParam.class))).thenThrow(
                new RefreshSessionException(RefreshSessionException.Reason.UNAVAILABLE, "redis"));

        RemoteServiceException failure = catchThrowableOfType(RemoteServiceException.class,
                () -> issuer.issueAuthorizedSession(param()));

        assertThat(failure.getHttpStatusCode()).isEqualTo(503);
        verify(access, never()).issueAccessToken(any());
    }

    private AuthorizedSessionIssueParam param() {
        return AuthorizedSessionIssueParam.builder().userId(USER).instanceId(INSTANCE)
                .roles(List.of("USER")).authVersion(4L).build();
    }

    private RefreshSessionReceipt receipt() {
        RefreshSessionRecord record = RefreshSessionRecord.builder()
                .recordVersion(1).userId(USER).instanceId(INSTANCE).roles(List.of("USER"))
                .authVersion(4L).currentRefreshTokenHash("a".repeat(64))
                .createdAt(NOW).idleExpiresAt(NOW.plus(RefreshSessionRecord.IDLE_LIFETIME))
                .absoluteExpiresAt(NOW.plus(RefreshSessionRecord.ABSOLUTE_LIFETIME)).build();
        return new RefreshSessionReceipt(SID, SID + "." + "a".repeat(64), record, NOW);
    }
}
