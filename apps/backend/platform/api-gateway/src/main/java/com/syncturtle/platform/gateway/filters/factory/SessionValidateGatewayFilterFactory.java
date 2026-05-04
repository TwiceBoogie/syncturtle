package com.syncturtle.platform.gateway.filters.factory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.platform.gateway.configurations.properties.SessionCookieProperties;
import com.syncturtle.platform.gateway.configurations.properties.SessionGatewayProperties;
import com.syncturtle.platform.gateway.enums.SessionType;
import com.syncturtle.platform.gateway.support.SessionRecord;
import com.syncturtle.platform.gateway.support.SessionStore;

import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

@Component
public class SessionValidateGatewayFilterFactory
        extends AbstractGatewayFilterFactory<SessionValidateGatewayFilterFactory.Config> {

    private final SessionStore sessions;
    private final SessionCookieProperties cookieProps;
    private final SessionGatewayProperties sessionProps;

    public SessionValidateGatewayFilterFactory(SessionStore sessions, SessionCookieProperties cookiesProps,
            SessionGatewayProperties sessionProps) {
        super(Config.class);
        this.sessions = sessions;
        this.cookieProps = cookiesProps;
        this.sessionProps = sessionProps;
    }

    @Getter
    @Setter
    public static final class Config {
        private boolean required = true;
        private String cookieName = "__Host-session";
        private SessionType sessionType = SessionType.USER;
        private List<String> requiredRoles = Collections.emptyList();
        private Duration sessionTtlOverride;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String sessionId = readCookie(exchange.getRequest().getCookies().get(config.getCookieName()));

            if (sessionId == null) {
                if (config.isRequired()) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                return chain.filter(exchange);
            }

            Instant now = Instant.now();

            return sessions.find(config.getSessionType(), sessionId)
                    // if missing session -> write 401 + stop chain (but type must stay
                    // Mono<SessionRecord>)
                    .switchIfEmpty(unauthorizedEmpty(exchange))
                    .flatMap(sess -> {
                        // session exists but invalid/expired -> 401
                        if (sess.isRevoked() || sess.getExpiresAt() == null || sess.getExpiresAt().isBefore(now)) {
                            return unauthorized(exchange);
                        }

                        // absolute expiration check -> delete then 401
                        if (sess.getAbsoluteExpiresAt() == null || !sess.getAbsoluteExpiresAt().isAfter(now)) {
                            return sessions.delete(config.getSessionType(), sessionId)
                                    .then(unauthorized(exchange));
                        }

                        // role check -> 403
                        if (config.getRequiredRoles() != null && !config.getRequiredRoles().isEmpty()) {
                            boolean ok = sess.getRoles() != null && sess.getRoles().stream()
                                    .anyMatch(config.getRequiredRoles()::contains);

                            if (!ok) {
                                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                                return exchange.getResponse().setComplete();
                            }
                        }

                        Duration ttl = config.getSessionTtlOverride() != null ? config.getSessionTtlOverride()
                                : sessionProps.getSessionTtl();

                        boolean needsRenew = shouldRenew(now, sess, ttl);

                        Mono<Void> maybeRenew = needsRenew ? renew(exchange, config, sessionId, sess, now, ttl)
                                : Mono.empty();

                        ServerHttpRequest mutated = exchange.getRequest().mutate()
                                .headers(h -> {
                                    h.set(GatewayHeaders.HDR_AUTH_USER_ID, sess.getUserId().toString());
                                    h.set(GatewayHeaders.HDR_AUTH_SESSION_TYPE, sess.getSessionType().name());
                                    h.set(GatewayHeaders.HDR_AUTH_SESSION_ID, sessionId);
                                })
                                .build();
                        return maybeRenew.then(chain.filter(exchange.mutate().request(mutated).build()));
                    });
        };
    }

    private Mono<Void> renew(ServerWebExchange exchange, Config config, String sessionId, SessionRecord current,
            Instant now, Duration ttl) {
        Instant requested = now.plus(ttl);
        Instant absolute = current.getAbsoluteExpiresAt();

        Instant newExpires = requested.isAfter(absolute) ? absolute : requested;
        if (current.getExpiresAt() == null || !newExpires.isAfter(current.getExpiresAt())) {
            return Mono.empty();
        }

        current.setExpiresAt(newExpires);
        Duration remainingAbsolute = Duration.between(now, absolute);

        return sessions.save(config.getSessionType(), sessionId, current, remainingAbsolute)
                .then(Mono.fromRunnable(() -> {
                    // renew cookie max-age to sliding ttl (not absolute)
                    exchange.getResponse()
                            .addCookie(SessionCookies.build(cookieProps, config.getCookieName(), sessionId, ttl));
                }));
    }

    private boolean shouldRenew(Instant now, SessionRecord sess, Duration ttl) {
        Duration remaining = Duration.between(now, sess.getExpiresAt());

        long thresholdMillis = (long) (ttl.toMillis() * sessionProps.getRenewWhenRemainingFraction());
        Duration threshold = Duration.ofMillis(thresholdMillis);

        // never less than 1 minute threshold to reduce churn
        if (threshold.compareTo(Duration.ofMinutes(1)) < 0) {
            threshold = Duration.ofMinutes(1);
        }
        return remaining.compareTo(threshold) < 0;
    }

    private static String readCookie(List<HttpCookie> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }
        String cookie = cookies.get(0).getValue();
        return (cookie == null || cookie.isBlank()) ? null : cookie.trim();
    }

    private static Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private static Mono<SessionRecord> unauthorizedEmpty(ServerWebExchange exchange) {
        return Mono.defer(() -> unauthorized(exchange).then(Mono.empty()));
    }
}
