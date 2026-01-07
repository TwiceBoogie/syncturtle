package com.syncturtle.platform.infra.gateway.filters.factory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import com.syncturtle.common.core.constants.GatewayHeaderNames;
import com.syncturtle.platform.infra.gateway.configurations.properties.SessionCookieProperties;
import com.syncturtle.platform.infra.gateway.configurations.properties.SessionGatewayProperties;
import com.syncturtle.platform.infra.gateway.enums.SessionType;
import com.syncturtle.platform.infra.gateway.support.SessionId;
import com.syncturtle.platform.infra.gateway.support.SessionRecord;
import com.syncturtle.platform.infra.gateway.support.SessionStore;

import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

@Component
public class SessionIssueGatewayFilterFactory
        extends AbstractGatewayFilterFactory<SessionIssueGatewayFilterFactory.Config> {

    private final SessionStore sessions;
    private final SessionCookieProperties cookieProps;
    private final SessionGatewayProperties sessionProps;

    public SessionIssueGatewayFilterFactory(SessionStore sessions, SessionCookieProperties cookieProps,
            SessionGatewayProperties sessionProps) {
        super(Config.class);
        this.sessions = sessions;
        this.cookieProps = cookieProps;
        this.sessionProps = sessionProps;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> chain.filter(exchange).then(Mono.defer(() -> {
            ServerHttpResponse response = exchange.getResponse();
            if (!isSuccessOrRedirect(response)) {
                // do not issue session on failures
                stripInternalHeaders(response);
                return Mono.empty();
            }

            String rawUserId = response.getHeaders().getFirst(GatewayHeaderNames.HDR_INTERNAL_USER_ID);
            String rawType = response.getHeaders().getFirst(GatewayHeaderNames.HDR_INTERNAL_SESSION_TYPE);
            String rolesCsv = response.getHeaders().getFirst(GatewayHeaderNames.HDR_INTERNAL_ROLES);

            // always strip internal headers from response
            stripInternalHeaders(response);

            if (rawUserId == null || rawUserId.isBlank()) {
                return Mono.empty();
            }

            UUID userId = UUID.fromString(rawUserId.trim());
            SessionType type = parseTypeOrDefault(rawType, config.getSessionType());

            Instant now = Instant.now();
            Duration ttl = config.getSessionTtlOverride() != null ? config.getSessionTtlOverride()
                    : sessionProps.getSessionTtl();
            Instant expiresAt = now.plus(ttl);
            Instant absoluteExpiresAt = now.plus(sessionProps.getAbsoluteTtl());

            SessionRecord record = new SessionRecord();
            record.setUserId(userId);
            record.setSessionType(type);
            record.setIssuedAt(now);
            record.setExpiresAt(expiresAt);
            record.setAbsoluteExpiresAt(absoluteExpiresAt);
            record.setRevoked(false);

            if (rolesCsv != null && !rolesCsv.isBlank()) {
                record.setRoles(List.of(rolesCsv.split(",")));
            }

            String sessionId = SessionId.newId();
            Duration remainingAbsolute = Duration.between(now, absoluteExpiresAt);

            return sessions.save(type, sessionId, record, remainingAbsolute)
                    .then(Mono.fromRunnable(() -> {
                        response.addCookie(SessionCookies.build(cookieProps, config.getCookieName(), sessionId, ttl));
                    }));
        }));
    }

    private static boolean isSuccessOrRedirect(ServerHttpResponse response) {
        HttpStatusCode status = response.getStatusCode();
        return status != null && (status.is2xxSuccessful() || status.is3xxRedirection());
    }

    private static void stripInternalHeaders(ServerHttpResponse response) {
        response.getHeaders().remove(GatewayHeaderNames.HDR_INTERNAL_USER_ID);
        response.getHeaders().remove(GatewayHeaderNames.HDR_INTERNAL_SESSION_TYPE);
        response.getHeaders().remove(GatewayHeaderNames.HDR_INTERNAL_ROLES);
    }

    private static SessionType parseTypeOrDefault(String raw, SessionType def) {
        if (raw == null || raw.isBlank()) {
            return def;
        }
        try {
            return SessionType.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return def;
        }
    }

    @Getter
    @Setter
    public static final class Config {
        private String cookieName = "__Host-session";
        private SessionType sessionType = SessionType.USER;
        private Duration sessionTtlOverride;
    }

}
