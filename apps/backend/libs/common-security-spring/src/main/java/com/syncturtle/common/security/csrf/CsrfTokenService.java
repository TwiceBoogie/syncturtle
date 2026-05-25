package com.syncturtle.common.security.csrf;

public interface CsrfTokenService {
    IssuedCsrfToken issueToken();

    boolean verify(String signedToken);

    String extractRawToken(String signedToken);

    boolean matches(String signedToken, String submittedRawToken);
}
