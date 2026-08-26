package com.syncturtle.platform.gateway.security;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.security.session.ParsedPassportSession;
import com.syncturtle.platform.gateway.security.session.PassportSessionValidator;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public final class PassportJwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private final PassportSessionValidator sessionValidator;
    private final PassportAuthenticationMetrics metrics;
    private final Clock clock;

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        PassportClaims claims;
        try {
            claims = PassportClaims.from(jwt, clock);
        } catch (PassportAuthenticationException exception) {
            recordFailure(exception);
            return Mono.error(exception);
        }

        return sessionValidator.validate(claims)
                .map(session -> authenticatedToken(jwt, session))
                .doOnSuccess(ignored -> metrics.accepted())
                .doOnError(PassportAuthenticationException.class, this::recordFailure);
    }

    private AbstractAuthenticationToken authenticatedToken(Jwt jwt, ParsedPassportSession session) {
        List<GrantedAuthority> authorities = new ArrayList<>(session.getRoles().size());
        for (String role : session.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return new PassportAuthenticationToken(jwt, authorities, session);
    }

    private void recordFailure(PassportAuthenticationException exception) {
        PassportAuthenticationFailureReason reason = exception.getReason();

        if (reason.isOperationalFailure()) {
            log.warn("Passport authentication dependency failure. reason={}", reason.metricValue());
            return;
        }

        log.debug("Passport authentication rejected. reason={}", reason.metricValue());
    }

}
