package com.syncturtle.services.user.support.fixture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.syncturtle.services.user.service.session.AuthenticatedSessionReceipt;
import com.syncturtle.services.user.service.session.IssuedRefreshTokenReceipt;
import com.syncturtle.services.user.service.token.IssuedAccessTokenReceipt;

public final class AuthenticatedSessionReceiptFixtures {

    private AuthenticatedSessionReceiptFixtures() {
    }

    public static AuthenticatedSessionReceipt issuedSession() {
        IssuedAccessTokenReceipt accessToken = mock(IssuedAccessTokenReceipt.class);
        IssuedRefreshTokenReceipt refreshToken = mock(IssuedRefreshTokenReceipt.class);

        when(accessToken.getToken()).thenReturn(AuthenticationFixtures.ACCESS_TOKEN);
        when(accessToken.getIssuedAt()).thenReturn(AuthenticationFixtures.ACCESS_ISSUED_AT);
        when(accessToken.getExpiresAt()).thenReturn(AuthenticationFixtures.ACCESS_EXPIRES_AT);

        when(refreshToken.getToken()).thenReturn(AuthenticationFixtures.REFRESH_TOKEN);
        when(refreshToken.getIssuedAt()).thenReturn(AuthenticationFixtures.REFRESH_ISSUED_AT);
        when(refreshToken.getExpiresAt()).thenReturn(AuthenticationFixtures.REFRESH_EXPIRES_AT);

        return AuthenticatedSessionReceipt.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

}
