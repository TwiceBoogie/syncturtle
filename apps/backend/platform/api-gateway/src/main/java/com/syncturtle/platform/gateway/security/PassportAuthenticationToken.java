package com.syncturtle.platform.gateway.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.Assert;

import com.syncturtle.platform.gateway.security.session.ParsedPassportSession;

import lombok.Getter;

@Getter
public final class PassportAuthenticationToken extends JwtAuthenticationToken {

    private final ParsedPassportSession validatedSession;

    public PassportAuthenticationToken(
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities,
            ParsedPassportSession validatedSession) {
        super(jwt, authorities, validatedSession == null ? null : validatedSession.getUserId());
        Assert.notNull(validatedSession, "validatedSession is required");

        this.validatedSession = validatedSession;
    }

}
