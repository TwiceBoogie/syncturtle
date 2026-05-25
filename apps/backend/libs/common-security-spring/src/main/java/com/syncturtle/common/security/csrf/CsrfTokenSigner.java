package com.syncturtle.common.security.csrf;

public interface CsrfTokenSigner {
    /**
     * Returns a token in the form {token}.{signature}
     * where signature = HMAC(token)
     * 
     * @param token
     * @return {token}.{signature}
     */
    String sign(String token);

    boolean verify(String tokenWithSignature);

    String extractToken(String tokenWithSignature);
}
