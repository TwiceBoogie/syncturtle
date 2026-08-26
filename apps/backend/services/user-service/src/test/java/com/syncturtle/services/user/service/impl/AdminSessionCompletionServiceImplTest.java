package com.syncturtle.services.user.service.impl;

import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.CLIENT_BINDING;
import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.INSTANCE_ID;
import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.USER_ID;
import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.claim;
import static com.syncturtle.services.user.support.fixture.AuthenticatedSessionReceiptFixtures.issuedSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.auth.session.PreAuthTransactionBinding;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.exception.AdminSessionHandoffException;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.AdminSessionCompletionService;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationResolver;
import com.syncturtle.services.user.service.collaborator.authorization.InstanceAuthorizationSnapshot;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffClaim;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffStore;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionReceipt;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionClientFingerprint;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionClientFingerprintFactory;
import com.syncturtle.services.user.service.param.AdminSessionCompletionParam;
import com.syncturtle.services.user.service.param.AuthorizedSessionIssueParam;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("AdminSessionCompletionService")
class AdminSessionCompletionServiceImplTest {

    private static final String SIGNED_CSRF = "signed.csrf";
    private static final String COMPLETION_CODE = "33333333-3333-3333-3333-333333333333.opaque";

    @Mock
    AdminSessionHandoffStore handoffStore;
    @Mock
    UserRepository userRepository;
    @Mock
    InstanceAuthorizationResolver authorizationResolver;
    @Mock
    RefreshSessionClientFingerprintFactory fingerprintFactory;
    @Mock
    AuthenticatedSessionIssuer sessionIssuer;
    @Mock
    PublicUrlResolver hostResolver;
    @Mock
    User user;
    @Captor
    ArgumentCaptor<AuthorizedSessionIssueParam> issueParamCaptor;

    private AdminSessionCompletionService service;

    @BeforeEach
    void setup() {
        service = new AdminSessionCompletionServiceImpl(
                handoffStore,
                userRepository,
                authorizationResolver,
                fingerprintFactory,
                sessionIssuer,
                hostResolver);
    }

    @Nested
    @DisplayName("complete(param)")
    class CompleteTests {

        @Test
        @DisplayName("issues with canonical roles and administrator version then consumes")
        void issuesWithCanonicalAuthorizationThenConsumes() {
            // arrange
            AdminSessionCompletionParam param = completionParam();
            AdminSessionHandoffClaim claimed = claim();
            InstanceAuthorizationSnapshot authorization = InstanceAuthorizationSnapshot.instanceAdmin(9L);
            AuthenticatedSessionReceipt issued = issuedSession();
            String preAuthHash = PreAuthTransactionBinding.fromValidatedSignedCsrfToken(SIGNED_CSRF).getValue();
            // conditions
            when(fingerprintFactory.create("192.0.2.10", "Admin browser"))
                    .thenReturn(new RefreshSessionClientFingerprint("Admin browser", CLIENT_BINDING));
            when(handoffStore.claim(COMPLETION_CODE, preAuthHash, CLIENT_BINDING)).thenReturn(claimed);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(user.isLoginAllowed()).thenReturn(true);
            when(user.getAuthVersion()).thenReturn(7L);
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID)).thenReturn(authorization);
            when(sessionIssuer.issueAuthorizedSession(any(AuthorizedSessionIssueParam.class)))
                    .thenReturn(issued);
            when(hostResolver.admin("/general")).thenReturn("https://admin.example.test/general");
            // act
            IssueTokenResponse result = service.complete(param);
            // assert
            assertThat(result.getLocation()).isEqualTo("https://admin.example.test/general");
            // verify
            verify(sessionIssuer).issueAuthorizedSession(issueParamCaptor.capture());
            assertThat(issueParamCaptor.getValue().getRoles()).containsExactly("USER", "INSTANCE_ADMIN");
            assertThat(issueParamCaptor.getValue().getAdminSessionVersion()).isEqualTo(9L);
            verify(handoffStore).consume(claimed);
            verify(handoffStore, never()).release(any());
        }

        @Test
        @DisplayName("consumes a deterministic missing-user claim without issuing")
        void consumesMissingUserClaim() {
            // arrange
            AdminSessionHandoffClaim claimed = claim();
            // conditions
            stubClaim(claimed);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
            // act + assert
            assertThatThrownBy(() -> service.complete(completionParam()))
                    .isInstanceOf(AdminSessionHandoffException.class);
            // verify
            verify(handoffStore).consume(claimed);
            verifyNoInteractions(authorizationResolver, sessionIssuer);
            verify(handoffStore, never()).release(any());
        }

        @Test
        @DisplayName("consumes login-disallowed and auth-version-mismatched claims")
        void consumesInvalidUserStateClaims() {
            // arrange
            AdminSessionHandoffClaim claimed = claim();
            // conditions
            stubClaim(claimed);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(user.isLoginAllowed()).thenReturn(false);
            // act + assert
            assertThatThrownBy(() -> service.complete(completionParam()))
                    .isInstanceOf(AdminSessionHandoffException.class);
            // verify
            verify(handoffStore).consume(claimed);
            verifyNoInteractions(authorizationResolver, sessionIssuer);
        }

        @Test
        @DisplayName("consumes an auth-version-mismatched claim")
        void consumesAuthVersionMismatch() {
            // arrange
            AdminSessionHandoffClaim claimed = claim();
            // conditions
            stubClaim(claimed);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(user.isLoginAllowed()).thenReturn(true);
            when(user.getAuthVersion()).thenReturn(8L);
            // act + assert
            assertThatThrownBy(() -> service.complete(completionParam()))
                    .isInstanceOf(AdminSessionHandoffException.class);
            // verify
            verify(handoffStore).consume(claimed);
            verifyNoInteractions(authorizationResolver, sessionIssuer);
        }

        @Test
        @DisplayName("consumes missing administrator authority and version mismatch")
        void consumesInvalidAdministratorAuthority() {
            // arrange
            AdminSessionHandoffClaim claimed = claim();
            // conditions
            stubClaim(claimed);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(user.isLoginAllowed()).thenReturn(true);
            when(user.getAuthVersion()).thenReturn(7L);
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID))
                    .thenReturn(InstanceAuthorizationSnapshot.member());
            // act + assert
            assertThatThrownBy(() -> service.complete(completionParam()))
                    .isInstanceOf(AdminSessionHandoffException.class);
            // verify
            verify(handoffStore).consume(claimed);
            verifyNoInteractions(sessionIssuer);
        }

        @Test
        @DisplayName("consumes an administrator-version-mismatched claim")
        void consumesAdministratorVersionMismatch() {
            // arrange
            AdminSessionHandoffClaim claimed = claim();
            // conditions
            stubClaim(claimed);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(user.isLoginAllowed()).thenReturn(true);
            when(user.getAuthVersion()).thenReturn(7L);
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID))
                    .thenReturn(InstanceAuthorizationSnapshot.instanceAdmin(10L));
            // act + assert
            assertThatThrownBy(() -> service.complete(completionParam()))
                    .isInstanceOf(AdminSessionHandoffException.class);
            // verify
            verify(handoffStore).consume(claimed);
            verifyNoInteractions(sessionIssuer);
        }

        @Test
        @DisplayName("releases a transient local revalidation failure before issuance")
        void releasesTransientLocalRevalidationFailure() {
            // arrange
            AdminSessionHandoffClaim claimed = claim();
            RuntimeException databaseFailure = new RuntimeException("database unavailable");
            // conditions
            stubClaim(claimed);
            when(userRepository.findById(USER_ID)).thenThrow(databaseFailure);
            // act + assert
            assertThatThrownBy(() -> service.complete(completionParam())).isSameAs(databaseFailure);
            // verify
            verify(handoffStore).release(claimed);
            verifyNoInteractions(authorizationResolver, sessionIssuer);
        }

        @Test
        @DisplayName("releases a transient remote revalidation failure before issuance")
        void releasesTransientRemoteRevalidationFailure() {
            // arrange
            AdminSessionHandoffClaim claimed = claim();
            RuntimeException remoteFailure = new RuntimeException("instance unavailable");
            // conditions
            stubClaim(claimed);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(user.isLoginAllowed()).thenReturn(true);
            when(user.getAuthVersion()).thenReturn(7L);
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID)).thenThrow(remoteFailure);
            // act + assert
            assertThatThrownBy(() -> service.complete(completionParam())).isSameAs(remoteFailure);
            // verify
            verify(handoffStore).release(claimed);
            verifyNoInteractions(sessionIssuer);
        }

        @Test
        @DisplayName("suppresses release failure on the original transient failure")
        void suppressesReleaseFailureOnOriginalFailure() {
            // arrange
            AdminSessionHandoffClaim claimed = claim();
            RuntimeException original = new RuntimeException("database unavailable");
            RuntimeException releaseFailure = new RuntimeException("Redis unavailable");
            // conditions
            stubClaim(claimed);
            when(userRepository.findById(USER_ID)).thenThrow(original);
            org.mockito.Mockito.doThrow(releaseFailure).when(handoffStore).release(claimed);
            // act
            Throwable result = org.assertj.core.api.Assertions.catchThrowable(
                    () -> service.complete(completionParam()));
            // assert
            assertThat(result).isSameAs(original);
            assertThat(result.getSuppressed()).containsExactly(releaseFailure);
        }

        @Test
        @DisplayName("never releases when issuance fails or is uncertain")
        void neverReleasesWhenIssuanceFails() {
            // arrange
            AdminSessionHandoffClaim claimed = claim();
            RuntimeException issuanceFailure = new RuntimeException("uncertain issuance");
            // conditions
            stubValidAuthority(claimed);
            when(sessionIssuer.issueAuthorizedSession(any(AuthorizedSessionIssueParam.class)))
                    .thenThrow(issuanceFailure);
            // act + assert
            assertThatThrownBy(() -> service.complete(completionParam())).isSameAs(issuanceFailure);
            // verify
            verify(handoffStore, never()).release(any());
            verify(handoffStore, never()).consume(any());
        }

        @Test
        @DisplayName("returns successfully issued credentials when consume cleanup fails")
        void returnsIssuedCredentialsWhenConsumeFails() {
            // arrange
            AdminSessionHandoffClaim claimed = claim();
            AuthenticatedSessionReceipt issued = issuedSession();
            // conditions
            stubValidAuthority(claimed);
            when(sessionIssuer.issueAuthorizedSession(any(AuthorizedSessionIssueParam.class)))
                    .thenReturn(issued);
            when(hostResolver.admin("/general")).thenReturn("https://admin.example.test/general");
            org.mockito.Mockito.doThrow(new RuntimeException("cleanup unavailable"))
                    .when(handoffStore).consume(claimed);
            // act
            IssueTokenResponse result = service.complete(completionParam());
            // assert
            assertThat(result.getAccessToken()).isNotBlank();
            assertThat(result.getRefreshToken()).isNotBlank();
            verify(handoffStore, never()).release(any());
        }

        @Test
        @DisplayName("rejects replay before any authority lookup or issuance")
        void rejectsReplayBeforeAuthorityLookupOrIssuance() {
            // arrange
            AdminSessionHandoffException replay = new AdminSessionHandoffException(
                    AdminSessionHandoffException.Reason.REPLAYED,
                    "replayed");
            String preAuthHash = PreAuthTransactionBinding.fromValidatedSignedCsrfToken(SIGNED_CSRF).getValue();
            // conditions
            when(fingerprintFactory.create("192.0.2.10", "Admin browser"))
                    .thenReturn(new RefreshSessionClientFingerprint("Admin browser", CLIENT_BINDING));
            when(handoffStore.claim(COMPLETION_CODE, preAuthHash, CLIENT_BINDING)).thenThrow(replay);
            // act + assert
            assertThatThrownBy(() -> service.complete(completionParam())).isSameAs(replay);
            // verify
            verifyNoInteractions(userRepository, authorizationResolver, sessionIssuer);
        }

        private void stubClaim(AdminSessionHandoffClaim claimed) {
            String preAuthHash = PreAuthTransactionBinding.fromValidatedSignedCsrfToken(SIGNED_CSRF).getValue();
            when(fingerprintFactory.create("192.0.2.10", "Admin browser"))
                    .thenReturn(new RefreshSessionClientFingerprint("Admin browser", CLIENT_BINDING));
            when(handoffStore.claim(COMPLETION_CODE, preAuthHash, CLIENT_BINDING)).thenReturn(claimed);
        }

        private void stubValidAuthority(AdminSessionHandoffClaim claimed) {
            stubClaim(claimed);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(user.isLoginAllowed()).thenReturn(true);
            when(user.getAuthVersion()).thenReturn(7L);
            when(authorizationResolver.resolve(USER_ID, INSTANCE_ID))
                    .thenReturn(InstanceAuthorizationSnapshot.instanceAdmin(9L));
        }

        private AdminSessionCompletionParam completionParam() {
            return AdminSessionCompletionParam.builder()
                    .completionCode(COMPLETION_CODE)
                    .signedCsrfToken(SIGNED_CSRF)
                    .clientIp("192.0.2.10")
                    .userAgent("Admin browser")
                    .build();
        }

    }

}
