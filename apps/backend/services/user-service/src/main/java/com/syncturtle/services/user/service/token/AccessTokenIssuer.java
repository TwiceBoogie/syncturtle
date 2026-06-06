package com.syncturtle.services.user.service.token;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.syncturtle.services.user.configuration.property.AuthProperties;
import com.syncturtle.services.user.configuration.property.KeyMaterial;
import com.syncturtle.services.user.configuration.property.PassportProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccessTokenIssuer {

    private final PassportProperties passportProperties;
    private final AuthProperties authProperties;
    private final KeyMaterial keyMaterial;
    private final Algorithm algorithm;

    public IssuedAccessTokenReceipt issueAccessToken(AccessTokenIssueSpec spec) {
        validate(spec);

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(authProperties.getAccessTokenTtl());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("typ", "JWT");

        if (passportProperties.hasKid()) {
            header.put("kid", passportProperties.getKid());
        }

        String[] roles = spec.getRoles().toArray(String[]::new);

        JWTCreator.Builder jwtBuilder = JWT.create()
                .withHeader(header)
                .withIssuer(passportProperties.getIssuer())
                .withAudience(passportProperties.getAudience())
                .withSubject(spec.getUserId())
                .withClaim("sid", spec.getSessionId())
                .withClaim("instance_id", spec.getInstanceId())
                .withArrayClaim("roles", roles)
                .withClaim("token_use", "access")
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt));

        if (spec.getUserAuthVersion() != null) {
            jwtBuilder.withClaim("auth_ver", spec.getUserAuthVersion());
        }

        if (spec.getAdminSessionVersion() != null) {
            jwtBuilder.withClaim("admin_session_ver", spec.getAdminSessionVersion());
        }

        String token = jwtBuilder.sign(algorithm);

        return IssuedAccessTokenReceipt.builder()
                .token(token)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    public String currentKid() {
        return passportProperties.getKid();
    }

    public String issuer() {
        return passportProperties.getIssuer();
    }

    public String audience() {
        return passportProperties.getAudience();
    }

    public KeyMaterial keyMaterial() {
        return keyMaterial;
    }

    private void validate(AccessTokenIssueSpec spec) {
        Assert.notNull(spec, "access token issue spec is required");
        Assert.hasText(spec.getUserId(), "userId is required");
        Assert.hasText(spec.getInstanceId(), "instanceId is required");
        Assert.hasText(spec.getSessionId(), "sessionId is required");
        Assert.notNull(spec.getRoles(), "roles is required");
        Assert.notNull(spec.getUserAuthVersion(), "userAuthVersion is required");
    }

}
