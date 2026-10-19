package com.syncturtle.services.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.web.error.exception.RemoteServiceException;
import com.syncturtle.services.user.exception.RefreshSessionException;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionStore;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceSignOutTest {

    private static final String PRESENTED = "33333333-3333-3333-3333-333333333333.presented";

    @Mock RefreshSessionStore store;
    @InjectMocks AuthenticationServiceImpl service;

    @Nested
    class SignOut {
        @Test
        void revokesOnlyThePresentedCurrentSession() {
            when(store.revokePresented(PRESENTED)).thenReturn(true);
            service.signOut(PRESENTED);
            verify(store).revokePresented(PRESENTED);
        }

        @Test
        void rejectsMissingCredentialWithoutRedisMutation() {
            AuthException failure = catchThrowableOfType(AuthException.class, () -> service.signOut(null));
            assertThat(failure.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED);
            verify(store, never()).revokePresented(PRESENTED);
        }

        @Test
        void staleCredentialCannotRevokeRotatedSuccessor() {
            when(store.revokePresented(PRESENTED)).thenReturn(false);
            AuthException failure = catchThrowableOfType(AuthException.class, () -> service.signOut(PRESENTED));
            assertThat(failure.getAuthErrorCode()).isEqualTo(AuthErrorCode.AUTHENTICATION_FAILED);
        }

        @Test
        void redisOutageIsOperationalFailure() {
            when(store.revokePresented(PRESENTED)).thenThrow(
                    new RefreshSessionException(RefreshSessionException.Reason.UNAVAILABLE, "redis unavailable"));
            RemoteServiceException failure = catchThrowableOfType(RemoteServiceException.class,
                    () -> service.signOut(PRESENTED));
            assertThat(failure.getErrorCode().getHttpStatusCode()).isEqualTo(503);
        }
    }
}
