package com.syncturtle.services.user.controller.internal;

import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.services.user.service.token.AccessTokenIssuer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final AccessTokenIssuer accessTokenIssuer;

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> jwks() {
        RSAPublicKey publicKey = accessTokenIssuer.keyMaterial().publicKey();

        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "kid", accessTokenIssuer.currentKid(),
                "use", "sig",
                "alg", "RS256",
                "n", toBase64Url(unsigned(publicKey.getModulus())),
                "e", toBase64Url(unsigned(publicKey.getPublicExponent())));

        Map<String, Object> body = Map.of("keys", List.of(jwk));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(15)).cachePublic())
                .body(body);
    }

    private static String toBase64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] unsigned(BigInteger bigInteger) {
        byte[] bytes = bigInteger.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return bytes;
    }

}
