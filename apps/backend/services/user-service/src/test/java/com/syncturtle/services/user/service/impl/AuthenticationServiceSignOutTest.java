package com.syncturtle.services.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.services.user.dto.response.SignOutResponse;
import com.syncturtle.services.user.mapper.UserApiMapper;
import com.syncturtle.services.user.messaging.kafka.factory.AuthenticationEmailEventFactory;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.repository.InstanceLiteRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.collaborator.authentication.provider.EmailPasswordCredentialProvider;
import com.syncturtle.services.user.service.collaborator.authentication.provider.MagicCodeCredentialProvider;
import com.syncturtle.services.user.service.collaborator.authentication.redirect.AuthenticationRedirector;
import com.syncturtle.services.user.service.collaborator.outbox.EmailOutboxWriter;
import com.syncturtle.services.user.service.collaborator.outbox.UserOutboxWriter;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionIssuer;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionFamilyStore;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceSignOutTest {

    private static final String REFRESH_TOKEN = "3a428175-bef1-4c13-ae53-997b6fbfc508.refresh-secret";
    private static final String REDIRECTION = "https://syncturtle.test/sign-in";

    @Mock
    AuthenticationRedirector authenticationRedirector;
    @Mock
    InstanceLiteRepository instanceRepository;
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
    RefreshSessionFamilyStore refreshSessionFamilyStore;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    UserEventFactory userEventFactory;
    @Mock
    UserOutboxWriter userOutboxWriter;
    @Mock
    AuthenticationEmailEventFactory emailEventFactory;
    @Mock
    EmailOutboxWriter emailOutboxWriter;
    @Mock
    UserApiMapper userApiMapper;

    @InjectMocks
    AuthenticationServiceImpl service;

    @Nested
    class SignOut {

        @Test
        void revokesPresentedSessionAndReturnsBoundaryResponse() {
            // arrange + conditions
            when(authenticationRedirector.signOutRedirect("WEB"))
                    .thenReturn(SignOutResponse.redirect(REDIRECTION));
            // act
            SignOutResponse result = service.signOut("WEB", REFRESH_TOKEN);
            // assert
            assertThat(result.getRedirection()).isEqualTo(REDIRECTION);
            // verify
            verify(refreshSessionFamilyStore).revokePresented(REFRESH_TOKEN);
        }

        @Test
        void completesBrowserLogoutWithoutARefreshCredential() {
            // arrange + conditions
            when(authenticationRedirector.signOutRedirect("ADMIN")).thenReturn(SignOutResponse.redirect(REDIRECTION));
            // act
            SignOutResponse result = service.signOut("ADMIN", null);
            // assert
            assertThat(result.getRedirection()).isEqualTo(REDIRECTION);
            // verify
            verify(refreshSessionFamilyStore, never()).revokePresented(any());
        }

        @Test
        void ignoresMalformedPresentedCredentialButDoesNotHideRedisFailure() {
            // arrange + conditions
            doThrow(new IllegalArgumentException("malformed"))
                    .when(refreshSessionFamilyStore)
                    .revokePresented(REFRESH_TOKEN);
            when(authenticationRedirector.signOutRedirect("ADMIN"))
                    .thenReturn(SignOutResponse.redirect(REDIRECTION));
            // act
            SignOutResponse result = service.signOut("ADMIN", REFRESH_TOKEN);
            // assert
            assertThat(result.getRedirection()).isEqualTo(REDIRECTION);
            // verify
            verify(refreshSessionFamilyStore).revokePresented(REFRESH_TOKEN);
        }

        @Test
        void propagatesServerSideRevocationFailure() {
            // arrange + conditions
            doThrow(new IllegalStateException("redis unavailable"))
                    .when(refreshSessionFamilyStore)
                    .revokePresented(REFRESH_TOKEN);
            // act
            IllegalStateException failure = catchThrowableOfType(
                    IllegalStateException.class,
                    () -> service.signOut("ADMIN", REFRESH_TOKEN));
            // assert
            assertThat(failure).hasMessage("redis unavailable");
            // verify
            verify(refreshSessionFamilyStore).revokePresented(REFRESH_TOKEN);
        }
    }
}
