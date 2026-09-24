package com.syncturtle.platform.gateway.security.csrf;

public interface GatewayCsrfTokenSigner {
    String sign(String token);

    boolean verify(String tokenWithSignature);

    String extractToken(String tokenWithSignature);
}
