package com.syncturtle.platform.infra.gateway.filters.factory;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import com.syncturtle.platform.infra.gateway.configurations.properties.SessionCookieProperties;
import com.syncturtle.platform.infra.gateway.enums.SessionType;
import com.syncturtle.platform.infra.gateway.support.SessionStore;

import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

@Component
public class SessionLogoutGatewayFilterFactory
        extends AbstractGatewayFilterFactory<SessionLogoutGatewayFilterFactory.Config> {

    private final SessionStore sessions;
    private final SessionCookieProperties cookieProps;

    public SessionLogoutGatewayFilterFactory(SessionStore sessions, SessionCookieProperties cookieProps) {
        super(Config.class);
        this.sessions = sessions;
        this.cookieProps = cookieProps;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> chain.filter(exchange).then(Mono.defer(() -> {
            ServerHttpResponse response = exchange.getResponse();
            if (!is2xx(response)) {
                return Mono.empty();
            }

            String sessionId = readCookie(exchange.getRequest().getCookies().get(config.getCookieName()));
            if (sessionId == null) {
                return Mono.empty();
            }

            return sessions.delete(config.getSessionType(), sessionId)
                    .then(Mono.fromRunnable(
                            () -> response.addCookie(SessionCookies.clear(cookieProps, config.getCookieName()))));
        }));
    }

    private static boolean is2xx(ServerHttpResponse response) {
        HttpStatusCode status = response.getStatusCode();
        return status != null && status.is2xxSuccessful();
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
