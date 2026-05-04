package com.syncturtle.services.user.services.impl;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.syncturtle.services.user.configurations.properties.AuthProperties;
import com.syncturtle.services.user.configurations.properties.KeyMaterial;
import com.syncturtle.services.user.configurations.properties.PassportProperties;
import com.syncturtle.services.user.dto.command.PassportTokenCommand;
import com.syncturtle.services.user.payload.IssuedPassport;
import com.syncturtle.services.user.services.PassportService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PassportServiceImpl implements PassportService {

    private final PassportProperties passportProperties;
    private final AuthProperties authProperties;
    private final KeyMaterial keyMaterial;
    private final Algorithm algorithm;

    @Override
    public IssuedPassport issueAccessToken(PassportTokenCommand command) {
        validate(command);

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(authProperties.getAccessTokenTtl());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("typ", "JWT");

        if (passportProperties.hasKid()) {
            header.put("kid", passportProperties.getKid());
        }

        String[] roles = command.getRoles().toArray(String[]::new);

        JWTCreator.Builder jwtBuilder = JWT.create()
                .withHeader(header)
                .withIssuer(passportProperties.getIssuer())
                .withAudience(passportProperties.getAudience())
                .withSubject(command.getUserId())
                .withClaim("sid", command.getSessionId())
                .withClaim("instance_id", command.getInstanceId())
                .withArrayClaim("roles", roles)
                .withClaim("token_use", "access")
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt));

        if (command.getUserAuthVersion() != null) {
            jwtBuilder.withClaim("auth_ver", command.getUserAuthVersion());
        }

        if (command.getAdminSessionVersion() != null) {
            jwtBuilder.withClaim("admin_session_ver", command.getAdminSessionVersion());
        }

        String token = jwtBuilder.sign(algorithm);

        return IssuedPassport.builder()
                .token(token)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public String currentKid() {
        return passportProperties.getKid();
    }

    @Override
    public String issuer() {
        return passportProperties.getIssuer();
    }

    @Override
    public String audience() {
        return passportProperties.getAudience();
    }

    @Override
    public KeyMaterial keyMaterial() {
        return keyMaterial;
    }

    private void validate(PassportTokenCommand command) {
        Assert.notNull(command, "command is required");
        Assert.hasText(command.getUserId(), "userId is required");
        Assert.hasText(command.getInstanceId(), "instanceId is required");
        Assert.hasText(command.getSessionId(), "sessionId is required");
        Assert.notNull(command.getRoles(), "roles is required");
        Assert.notNull(command.getUserAuthVersion(), "userAuthVersion is required");
    }

}
