package com.syncturtle.common.web.csrf;

public interface CsrfTokenService {
    IssuedCsrfToken issueToken();

    boolean verify(String signedToken);

    String extractRawToken(String signedToken);

    boolean matches(String signedToken, String submittedRawToken);
}
