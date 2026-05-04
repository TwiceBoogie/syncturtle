package com.syncturtle.platform.gateway.filters.factory;

import java.time.Duration;
import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import com.syncturtle.platform.gateway.configurations.properties.SessionCookieProperties;
import com.syncturtle.platform.gateway.enums.SessionType;
import com.syncturtle.platform.gateway.support.SessionStore;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class SessionLogoutGatewayFilterFactory
        extends AbstractGatewayFilterFactory<SessionLogoutGatewayFilterFactory.Config> {

    private static final Duration DELETE_TIMEOUT = Duration.ofMillis(500);

    private final SessionStore sessions;
    private final SessionCookieProperties cookieProps;

    public SessionLogoutGatewayFilterFactory(SessionStore sessions, SessionCookieProperties cookieProps) {
        super(Config.class);
        this.sessions = sessions;
        this.cookieProps = cookieProps;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpResponse response = exchange.getResponse();

            // read cookie from incoming request
            String sessionId = readCookie(exchange.getRequest().getCookies().get(config.getCookieName()));

            // always clear cookie if present
            if (sessionId != null) {
                response.beforeCommit(() -> {
                    response.addCookie(SessionCookies.clear(cookieProps, config.getCookieName()));
                    return Mono.empty();
                });
            }

            // never fail logout if redis is down, timeout, etc
            Mono<Void> deleteSessionMono = (sessionId == null)
                    ? Mono.empty()
                    : sessions.delete(config.getSessionType(), sessionId)
                            .timeout(DELETE_TIMEOUT)
                            .doOnSuccess(v -> log.debug("Deleted session: type={}, cookie={}", config.getSessionType(),
                                    config.getCookieName()))
                            .onErrorResume(ex -> {
                                log.warn("Failed to delete session (continuing logout): type={}, cookie={}, err={}",
                                        config.getSessionType(), config.getCookieName(), ex.toString());
                                return Mono.empty();
                            });

            return chain.filter(exchange)
                    .then(deleteSessionMono)
                    .onErrorResume(ex -> deleteSessionMono.then(Mono.error(ex)));
        };
    }

    private static String readCookie(List<HttpCookie> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }
        String v = cookies.get(0).getValue();
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    @Getter
    @Setter
    public static final class Config {
        private String cookieName = "__Host-session";
        private SessionType sessionType = SessionType.USER;
    }

}
