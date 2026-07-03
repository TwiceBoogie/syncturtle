package com.syncturtle.services.user.service.impl;

import static com.syncturtle.services.user.testsupport.assertion.CredentialAuthenticationSpecAssert.assertThatCredentialSpec;
import static com.syncturtle.services.user.testsupport.factory.AuthenticationMockFactory.authConfig;
import static com.syncturtle.services.user.testsupport.factory.AuthenticationMockFactory.configuredInstance;
import static com.syncturtle.services.user.testsupport.factory.AuthenticationMockFactory.configuredInstanceWithId;
import static com.syncturtle.services.user.testsupport.factory.AuthenticationMockFactory.passwordAutosetProjection;
import static com.syncturtle.services.user.testsupport.factory.AuthenticationMockFactory.unconfiguredInstance;
import static com.syncturtle.services.user.testsupport.factory.AuthenticationMockFactory.user;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticatedSessionReceiptFixtures.issuedSession;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.ACCESS_EXPIRES_AT;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.ACCESS_ISSUED_AT;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.ACCESS_TOKEN;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.CLIENT_IP;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.EMAIL;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.EMAIL_WITH_WHITESPACE_AND_CASE;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.FAILURE_LOCATION;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.INSTANCE_ID;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.NEXT_PATH;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.NORMALIZED_EMAIL;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.PASSWORD;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.REFRESH_EXPIRES_AT;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.REFRESH_ISSUED_AT;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.REFRESH_TOKEN;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.SUCCESS_LOCATION;
import static com.syncturtle.services.user.testsupport.fixture.AuthenticationFixtures.USER_AGENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.services.user.dto.response.EmailCheckResponse;
import com.syncturtle.services.user.dto.response.IssueTokenResponse;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.messaging.outbox.UserOutboxWriter;
import com.syncturtle.services.user.model.Instance;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.repository.projection.UserPasswordAutosetProjection;
import com.syncturtle.services.user.service.AuthenticationService;
import com.syncturtle.services.user.service.authentication.provider.CredentialAuthenticationReceipt;
import com.syncturtle.services.user.service.authentication.provider.CredentialAuthenticationSpec;
import com.syncturtle.services.user.service.authentication.provider.EmailPasswordCredentialProvider;
import com.syncturtle.services.user.service.authentication.provider.MagicCodeCredentialProvider;
import com.syncturtle.services.user.service.authentication.redirect.AuthenticationRedirector;
import com.syncturtle.services.user.service.mapper.UserApiMapper;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.service.runtime.UserAuthRuntimeSnapshot;
import com.syncturtle.services.user.service.session.AuthenticatedSessionIssueSpec;
import com.syncturtle.services.user.service.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.session.AuthenticatedSessionReceipt;
import com.syncturtle.services.user.service.session.RefreshSessionTokenStore;
import com.syncturtle.services.user.type.AuthenticationFlowType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("AuthenticationService")
class AuthenticationServiceImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    AuthenticationRedirector authenticationRedirector;
    @Mock
    InstanceRepository instanceRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    RequestClientContext clientContext;
    @Mock
    UserAuthRuntimeConfigResolver configFlagResolver;
    @Mock
    EmailPasswordCredentialProvider emailPasswordProvider;
    @Mock
    MagicCodeCredentialProvider magicCodeProvider;
    @Mock
    AuthenticatedSessionIssuer authenticatedSessionIssuer;
    @Mock
    RefreshSessionTokenStore refreshSessionTokenStore;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    UserEventFactory userEventFactory;
    @Mock
    UserOutboxWriter outboxWriter;
    @Mock
    UserApiMapper userApiMapper;
    @Captor
    ArgumentCaptor<CredentialAuthenticationSpec> credentialSpecCaptor;
    @Captor
    ArgumentCaptor<AuthenticatedSessionIssueSpec> sessionSpecCaptor;

    private AuthenticationService service;

    @BeforeEach
    void setup() {
        service = new AuthenticationServiceImpl(
                authenticationRedirector,
                instanceRepository,
                userRepository,
                clientContext,
                configFlagResolver,
                emailPasswordProvider,
                magicCodeProvider,
                authenticatedSessionIssuer,
                refreshSessionTokenStore,
                passwordEncoder,
                userEventFactory,
                outboxWriter,
                userApiMapper);
    }

    @Nested
    @DisplayName("emailCheck(String)")
    class EmailCheckTests {

        @Test
        @DisplayName("returns MAGIC_CODE for new user when signup and magic link are enabled")
        void returnsMagicCodeForNewUserWhenSignupAndMagicLinkAreEnabled() {
            // arrange
            Instance instance = configuredInstance();
            UserAuthRuntimeSnapshot config = authConfig(true, true);

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            when(configFlagResolver.getInstanceConfigurations()).thenReturn(config);
            when(userRepository.findByEmailIgnoreCase(NORMALIZED_EMAIL, UserPasswordAutosetProjection.class))
                    .thenReturn(Optional.empty());

            // act
            EmailCheckResponse actual = service.emailCheck(EMAIL_WITH_WHITESPACE_AND_CASE);

            // assert
            assertThat(actual.isExistingUser()).isFalse();
            assertThat(actual.getAuthenticationFlow()).isEqualTo(AuthenticationFlowType.MAGIC_CODE);

            // verify
            verify(instanceRepository).findFirstByOrderByCreatedAtAsc();
            verify(configFlagResolver).getInstanceConfigurations();
            verify(userRepository).findByEmailIgnoreCase(NORMALIZED_EMAIL, UserPasswordAutosetProjection.class);
        }

        @Test
        @DisplayName("returns CREDENTIAL for new user when magic link is unavailable")
        void returnsCredentialForNewUserWhenMagicLinkIsUnavailable() {
            // arrange
            Instance instance = configuredInstance();
            UserAuthRuntimeSnapshot config = authConfig(true, false);

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            when(configFlagResolver.getInstanceConfigurations()).thenReturn(config);
            when(userRepository.findByEmailIgnoreCase(EMAIL, UserPasswordAutosetProjection.class))
                    .thenReturn(Optional.empty());

            // act
            EmailCheckResponse actual = service.emailCheck(EMAIL);

            // assert
            assertThat(actual.isExistingUser()).isFalse();
            assertThat(actual.getAuthenticationFlow()).isEqualTo(AuthenticationFlowType.CREDENTIAL);

            // verify
            verify(userRepository).findByEmailIgnoreCase(EMAIL, UserPasswordAutosetProjection.class);
        }

        @Test
        @DisplayName("returns MAGIC_CODE for existing autoset-password user when magic link is available")
        void returnsMagicCodeForExistingAutosetPasswordUserWhenMagicLinkIsAvailable() {
            // arrange
            Instance instance = configuredInstance();
            UserAuthRuntimeSnapshot config = authConfig(true, true);
            UserPasswordAutosetProjection projection = passwordAutosetProjection(true);

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            when(configFlagResolver.getInstanceConfigurations()).thenReturn(config);
            when(userRepository.findByEmailIgnoreCase(EMAIL, UserPasswordAutosetProjection.class))
                    .thenReturn(Optional.of(projection));

            // act
            EmailCheckResponse actual = service.emailCheck(EMAIL);

            // assert
            assertThat(actual.isExistingUser()).isTrue();
            assertThat(actual.getAuthenticationFlow()).isEqualTo(AuthenticationFlowType.MAGIC_CODE);

            // verify
            verify(userRepository).findByEmailIgnoreCase(EMAIL, UserPasswordAutosetProjection.class);
        }

        @Test
        @DisplayName("returns CREDENTIAL for existing user when password is not autoset")
        void returnsCredentialForExistingUserWhenPasswordIsNotAutoset() {
            // arrange
            Instance instance = configuredInstance();
            UserAuthRuntimeSnapshot config = authConfig(true, true);
            UserPasswordAutosetProjection projection = passwordAutosetProjection(false);

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            when(configFlagResolver.getInstanceConfigurations()).thenReturn(config);
            when(userRepository.findByEmailIgnoreCase(EMAIL, UserPasswordAutosetProjection.class))
                    .thenReturn(Optional.of(projection));

            // act
            EmailCheckResponse actual = service.emailCheck(EMAIL);

            // assert
            assertThat(actual.isExistingUser()).isTrue();
            assertThat(actual.getAuthenticationFlow()).isEqualTo(AuthenticationFlowType.CREDENTIAL);
        }

        @Test
        @DisplayName("throws AuthException when instance is not configured")
        void throwsAuthExceptionWhenInstanceIsNotConfigured() {
            // arrange
            Instance instance = unconfiguredInstance();

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));

            // act + assert
            assertThatThrownBy(() -> service.emailCheck(EMAIL))
                    .isInstanceOf(AuthException.class);

            // verify
            verify(instanceRepository).findFirstByOrderByCreatedAtAsc();
            verifyNoInteractions(configFlagResolver, userRepository);
        }

        @Test
        @DisplayName("throws AuthException for invalid email after instance setup check")
        void throwsAuthExceptionForInvalidEmailAfterInstanceSetupCheck() {
            // arrange
            Instance instance = configuredInstance();

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));

            // act + assert
            assertThatThrownBy(() -> service.emailCheck("not-an-email"))
                    .isInstanceOf(AuthException.class);

            // verify
            verify(instanceRepository).findFirstByOrderByCreatedAtAsc();
            verifyNoInteractions(configFlagResolver, userRepository);
        }

    }

    @Nested
    @DisplayName("emailPasswordSignIn(String, String, String)")
    class EmailPasswordSignInTests {

        @Test
        @DisplayName("authenticates existing user and issues tokens")
        void authenticatesExistingUserAndIssuesTokens() {
            // arrange
            Instance instance = configuredInstanceWithId();
            User user = user();
            AuthenticatedSessionReceipt receipt = issuedSession();

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            when(clientContext.getClientIp()).thenReturn(CLIENT_IP);
            when(clientContext.getUserAgent()).thenReturn(USER_AGENT);
            when(emailPasswordProvider.authenticate(any(CredentialAuthenticationSpec.class)))
                    .thenReturn(CredentialAuthenticationReceipt.existingUser(user));
            when(authenticatedSessionIssuer.issueNewSession(any(AuthenticatedSessionIssueSpec.class)))
                    .thenReturn(receipt);
            when(authenticationRedirector.successLocation(NEXT_PATH)).thenReturn(SUCCESS_LOCATION);

            // act
            IssueTokenResponse actual = service.emailPasswordSignIn(EMAIL, PASSWORD, NEXT_PATH);

            // assert
            assertIssuedTokenResponse(actual);

            // verify
            verify(emailPasswordProvider).authenticate(credentialSpecCaptor.capture());
            assertThatCredentialSpec(credentialSpecCaptor.getValue())
                    .hasEmail(EMAIL)
                    .hasSecret(PASSWORD)
                    .hasSignup(false)
                    .hasIpAddress(CLIENT_IP)
                    .hasUserAgent(USER_AGENT);

            verify(authenticatedSessionIssuer).issueNewSession(sessionSpecCaptor.capture());
            assertThat(sessionSpecCaptor.getValue().getUser()).isSameAs(user);
            assertThat(sessionSpecCaptor.getValue().getInstanceId()).isEqualTo(INSTANCE_ID);
            assertThat(sessionSpecCaptor.getValue().getIpAddress()).isEqualTo(CLIENT_IP);
            assertThat(sessionSpecCaptor.getValue().getUserAgent()).isEqualTo(USER_AGENT);

            InOrder order = inOrder(instanceRepository, emailPasswordProvider, authenticatedSessionIssuer,
                    authenticationRedirector);
            order.verify(instanceRepository).findFirstByOrderByCreatedAtAsc();
            order.verify(emailPasswordProvider).authenticate(any(CredentialAuthenticationSpec.class));
            order.verify(authenticatedSessionIssuer).issueNewSession(any(AuthenticatedSessionIssueSpec.class));
            order.verify(authenticationRedirector).successLocation(NEXT_PATH);

            verifyNoInteractions(magicCodeProvider, refreshSessionTokenStore, passwordEncoder, userApiMapper);
        }

        @Test
        @DisplayName("returns sign-in failure redirect when input is missing")
        void returnsSignInFailureRedirectWhenInputIsMissing() {
            // arrange
            Instance instance = configuredInstance();
            IssueTokenResponse failure = IssueTokenResponse.redirect(FAILURE_LOCATION);

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            when(authenticationRedirector.signInFailure(any(AuthException.class), eq(NEXT_PATH)))
                    .thenReturn(failure);

            // act
            IssueTokenResponse actual = service.emailPasswordSignIn("", PASSWORD, NEXT_PATH);

            // assert
            assertThat(actual).isSameAs(failure);

            // verify
            verify(authenticationRedirector).signInFailure(any(AuthException.class), eq(NEXT_PATH));
            verifyNoInteractions(emailPasswordProvider, magicCodeProvider, authenticatedSessionIssuer,
                    refreshSessionTokenStore, passwordEncoder, userApiMapper);
        }

        @Test
        @DisplayName("returns sign-in failure redirect when provider rejects credentials")
        void returnsSignInFailureRedirectWhenProviderRejectsCredentials() {
            // arrange
            Instance instance = configuredInstance();
            AuthException failureException = AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED_SIGN_IN);
            IssueTokenResponse failure = IssueTokenResponse.redirect(FAILURE_LOCATION);

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            when(clientContext.getClientIp()).thenReturn(CLIENT_IP);
            when(clientContext.getUserAgent()).thenReturn(USER_AGENT);
            when(emailPasswordProvider.authenticate(any(CredentialAuthenticationSpec.class)))
                    .thenThrow(failureException);
            when(authenticationRedirector.signInFailure(same(failureException), eq(NEXT_PATH)))
                    .thenReturn(failure);

            // act
            IssueTokenResponse actual = service.emailPasswordSignIn(EMAIL, PASSWORD, NEXT_PATH);

            // assert
            assertThat(actual).isSameAs(failure);

            // verify
            verify(authenticationRedirector).signInFailure(same(failureException), eq(NEXT_PATH));
            verifyNoInteractions(magicCodeProvider, authenticatedSessionIssuer, refreshSessionTokenStore,
                    passwordEncoder, userApiMapper);
        }

    }

    @Nested
    @DisplayName("emailPasswordSignUp(String, String, String)")
    class emailPasswordSignUpTests {

        @Test
        @DisplayName("authenticates signup request and issues tokens")
        void authenticatesSignupRequestAndIssuesTokens() {
            // arrange
            Instance instance = configuredInstanceWithId();
            User user = user();
            AuthenticatedSessionReceipt receipt = issuedSession();

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            when(clientContext.getClientIp()).thenReturn(CLIENT_IP);
            when(clientContext.getUserAgent()).thenReturn(USER_AGENT);
            when(emailPasswordProvider.authenticate(any(CredentialAuthenticationSpec.class)))
                    .thenReturn(CredentialAuthenticationReceipt.createdUser(user));
            when(authenticatedSessionIssuer.issueNewSession(any(AuthenticatedSessionIssueSpec.class)))
                    .thenReturn(receipt);
            when(authenticationRedirector.successLocation(NEXT_PATH)).thenReturn(SUCCESS_LOCATION);

            // act
            IssueTokenResponse actual = service.emailPasswordSignUp(EMAIL, PASSWORD, NEXT_PATH);

            // assert
            assertIssuedTokenResponse(actual);

            // verify
            verify(emailPasswordProvider).authenticate(credentialSpecCaptor.capture());
            assertThatCredentialSpec(credentialSpecCaptor.getValue())
                    .hasEmail(EMAIL)
                    .hasSecret(PASSWORD)
                    .hasSignup(true)
                    .hasIpAddress(CLIENT_IP)
                    .hasUserAgent(USER_AGENT);

            verify(authenticatedSessionIssuer).issueNewSession(sessionSpecCaptor.capture());
            assertThat(sessionSpecCaptor.getValue().getUser()).isSameAs(user);
            assertThat(sessionSpecCaptor.getValue().getInstanceId()).isEqualTo(INSTANCE_ID);
        }

        @Test
        @DisplayName("returns sign-up failure redirect when provider rejects signup")
        void returnsSignUpFailureRedirectWhenProviderRejectsSignup() {
            // arrange
            Instance instance = configuredInstance();
            AuthException failureException = AuthException.of(AuthErrorCode.AUTHENTICATION_FAILED);
            IssueTokenResponse failure = IssueTokenResponse.redirect(FAILURE_LOCATION);

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            when(clientContext.getClientIp()).thenReturn(CLIENT_IP);
            when(clientContext.getUserAgent()).thenReturn(USER_AGENT);
            when(emailPasswordProvider.authenticate(any(CredentialAuthenticationSpec.class)))
                    .thenThrow(failureException);
            when(authenticationRedirector.signUpFailure(same(failureException), eq(NEXT_PATH)))
                    .thenReturn(failure);

            // act
            IssueTokenResponse actual = service.emailPasswordSignUp(EMAIL, PASSWORD, NEXT_PATH);

            // assert
            assertThat(actual).isSameAs(failure);

            // verify
            verify(authenticationRedirector).signUpFailure(same(failureException), eq(NEXT_PATH));
            verifyNoInteractions(magicCodeProvider, authenticatedSessionIssuer, refreshSessionTokenStore,
                    passwordEncoder, userApiMapper);
        }

    }

    private static void assertIssuedTokenResponse(IssueTokenResponse actual) {
        assertThat(actual.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(actual.getRefreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(actual.getAccessIssuedAt()).isEqualTo(ACCESS_ISSUED_AT);
        assertThat(actual.getAccessExpiresAt()).isEqualTo(ACCESS_EXPIRES_AT);
        assertThat(actual.getRefreshIssuedAt()).isEqualTo(REFRESH_ISSUED_AT);
        assertThat(actual.getRefreshExpiresAt()).isEqualTo(REFRESH_EXPIRES_AT);
        assertThat(actual.getLocation()).isEqualTo(SUCCESS_LOCATION);
    }

}
