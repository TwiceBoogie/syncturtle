package com.syncturtle.common.web.csrf.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.util.StringUtils;

import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.web.csrf.CsrfTokenService;
import com.syncturtle.common.web.csrf.CsrfTokenSigner;
import com.syncturtle.common.web.csrf.IssuedCsrfToken;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class CsrfTokenServiceImpl implements CsrfTokenService {

    private final SecureTokenGenerator tokenGenerator;
    private final CsrfTokenSigner tokenSigner;
    private final int rawTokenBytes;

    @Override
    public IssuedCsrfToken issueToken() {
        String rawToken = tokenGenerator.generateBase64Url(rawTokenBytes);
        String signedToken = tokenSigner.sign(rawToken);
        return new IssuedCsrfToken(rawToken, signedToken);
    }

    @Override
    public boolean verify(String signedToken) {
        return tokenSigner.verify(signedToken);
    }

    @Override
    public String extractRawToken(String signedToken) {
        return tokenSigner.extractToken(signedToken);
    }

    @Override
    public boolean matches(String signedToken, String submittedRawToken) {
        if (!StringUtils.hasText(signedToken) || !StringUtils.hasText(submittedRawToken)) {
            return false;
        }

        if (!verify(signedToken)) {
            return false;
        }

        String expectedRawToken = extractRawToken(signedToken);
        if (!StringUtils.hasText(expectedRawToken)) {
            return false;
        }

        return constantTimeEquals(expectedRawToken, submittedRawToken);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

}
