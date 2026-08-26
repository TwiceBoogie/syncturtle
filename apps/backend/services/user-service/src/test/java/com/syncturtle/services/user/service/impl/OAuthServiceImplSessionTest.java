package com.syncturtle.services.user.service.impl;

import static com.syncturtle.services.user.support.fixture.AuthenticatedSessionReceiptFixtures.issuedSession;
import static com.syncturtle.services.user.support.fixture.AuthenticationFixtures.ACCESS_TOKEN;
import static com.syncturtle.services.user.support.fixture.AuthenticationFixtures.REFRESH_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.auth.provider.AuthProvider;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.model.InstanceLite;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceLiteRepository;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.OAuthAuthenticationReceipt;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.OAuthState;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.OAuthStateStore;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.github.GitHubOAuthCredentialProvider;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.gitlab.GitLabOAuthCredentialProvider;
import com.syncturtle.services.user.service.collaborator.authentication.oauth.google.GoogleOAuthCredentialProvider;
import com.syncturtle.services.user.service.collaborator.authentication.redirect.AuthenticationRedirector;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionReceipt;
import com.syncturtle.services.user.service.param.AuthenticatedSessionIssueParam;
import com.syncturtle.services.user.service.param.OAuthAuthenticationParam;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class OAuthServiceImplSessionTest {

    private static final String CODE = "oauth-code";
    private static final String STATE = "oauth-state";
    private static final String NEXT_PATH = "/projects";
    private static final String SUCCESS_LOCATION = "https://app.syncturtle.test/projects";
    private static final String CLIENT_IP = "192.0.2.10";
    private static final String USER_AGENT = "Test browser";
    private static final UUID INSTANCE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private OAuthStateStore oAuthStateStore;
    @Mock
    private GoogleOAuthCredentialProvider googleProvider;
    @Mock
    private GitHubOAuthCredentialProvider githubProvider;
    @Mock
    private GitLabOAuthCredentialProvider gitlabProvider;
    @Mock
    private UserAuthRuntimeConfigResolver configFlagResolver;
    @Mock
    private InstanceLiteRepository instanceRepository;
    @Mock
    private AuthenticationRedirector authenticationRedirector;
    @Mock
    private AuthenticatedSessionIssuer authenticatedSessionIssuer;
    @Mock
    private RequestClientContext clientContext;

    private OAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OAuthServiceImpl(
                oAuthStateStore,
                googleProvider,
                githubProvider,
                gitlabProvider,
                configFlagResolver,
                instanceRepository,
                authenticationRedirector,
                authenticatedSessionIssuer,
                clientContext);
    }

    @Nested
    class GoogleOAuthCallback {

        @Test
        void issuesTheAuthenticatedUserThroughTheSharedV2SessionIssuer() {
            // arrange
            User user = mock(User.class);
            givenCallbackContext();
            when(oAuthStateStore.consume(STATE, AuthProvider.GOOGLE)).thenReturn(Optional.of(oAuthState()));
            when(googleProvider.authenticate(any(OAuthAuthenticationParam.class)))
                    .thenReturn(OAuthAuthenticationReceipt.existingUser(user));

            // act
            IssueTokenResponse result = service.googleOAuthCallback(CODE, STATE);

            // assert
            assertSuccessfulV2Issuance(result, user);
        }
    }

    @Nested
    class GitHubOAuthCallback {

        @Test
        void issuesTheAuthenticatedUserThroughTheSharedV2SessionIssuer() {
            // arrange
            User user = mock(User.class);
            givenCallbackContext();
            when(oAuthStateStore.consume(STATE, AuthProvider.GITHUB)).thenReturn(Optional.of(oAuthState()));
            when(githubProvider.authenticate(any(OAuthAuthenticationParam.class)))
                    .thenReturn(OAuthAuthenticationReceipt.existingUser(user));

            // act
            IssueTokenResponse result = service.githubOAuthCallback(CODE, STATE);

            // assert
            assertSuccessfulV2Issuance(result, user);
        }
    }

    @Nested
    class GitLabOAuthCallback {

        @Test
        void issuesTheAuthenticatedUserThroughTheSharedV2SessionIssuer() {
            // arrange
            User user = mock(User.class);
            givenCallbackContext();
            when(oAuthStateStore.consume(STATE, AuthProvider.GITLAB)).thenReturn(Optional.of(oAuthState()));
            when(gitlabProvider.authenticate(any(OAuthAuthenticationParam.class)))
                    .thenReturn(OAuthAuthenticationReceipt.existingUser(user));

            // act
            IssueTokenResponse result = service.gitlabOAuthCallback(CODE, STATE);

            // assert
            assertSuccessfulV2Issuance(result, user);
        }
    }

    private void givenCallbackContext() {
        InstanceLite instance = mock(InstanceLite.class);
        AuthenticatedSessionReceipt session = issuedSession();
        when(instance.isSetupDone()).thenReturn(true);
        when(instance.getId()).thenReturn(INSTANCE_ID);
        when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
        when(clientContext.getClientIp()).thenReturn(CLIENT_IP);
        when(clientContext.getUserAgent()).thenReturn(USER_AGENT);
        when(authenticatedSessionIssuer.issueNewSession(any(AuthenticatedSessionIssueParam.class)))
                .thenReturn(session);
        when(authenticationRedirector.successLocation(NEXT_PATH)).thenReturn(SUCCESS_LOCATION);
    }

    private void assertSuccessfulV2Issuance(IssueTokenResponse result, User expectedUser) {
        ArgumentCaptor<AuthenticatedSessionIssueParam> issueParam = ArgumentCaptor.forClass(
                AuthenticatedSessionIssueParam.class);
        verify(authenticatedSessionIssuer).issueNewSession(issueParam.capture());
        assertThat(issueParam.getValue().getUser()).isSameAs(expectedUser);
        assertThat(issueParam.getValue().getInstanceId()).isEqualTo(INSTANCE_ID);
        assertThat(issueParam.getValue().getIpAddress()).isEqualTo(CLIENT_IP);
        assertThat(issueParam.getValue().getUserAgent()).isEqualTo(USER_AGENT);
        assertThat(result.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(result.getLocation()).isEqualTo(SUCCESS_LOCATION);
    }

    private OAuthState oAuthState() {
        return OAuthState.builder()
                .state(STATE)
                .nextPath(NEXT_PATH)
                .build();
    }
}
