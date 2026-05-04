package com.syncturtle.platform.gateway.filters.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PassportSessionValidationWebFilter implements WebFilter {

    private static final String INSTANCE_ADMIN_ROLE = "INSTANCE_ADMIN";

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> validate(exchange, authentication.getToken()))
                .then(chain.filter(exchange))
                .onErrorResume(PassportSessionValidationException.class,
                        exception -> unauthorized(exchange, exception.getMessage()))
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> validate(ServerWebExchange exchange, Jwt jwt) {
        String sessionId = jwt.getClaimAsString("sid");
        String userId = jwt.getSubject();
        String instanceId = jwt.getClaimAsString("instance_id");
        Long jwtAuthVersion = claimAsLong(jwt, "auth_ver");
        Long jwtAdminSessionVersion = claimAsLong(jwt, "admin_session_ver");
        List<String> roles = jwt.getClaimAsStringList("roles");

        if (!StringUtils.hasText(sessionId)) {
            return invalid("Missing session id");
        }

        if (!StringUtils.hasText(userId)) {
            return invalid("Missing user id");
        }

        if (!StringUtils.hasText(instanceId)) {
            return invalid("Missing instance id");
        }

        if (jwtAuthVersion == null) {
            return invalid("Missing auth version");
        }

        return redis.opsForValue()
                .get(sessionKey(sessionId))
                .switchIfEmpty(Mono.error(new PassportSessionValidationException("Session not found")))
                .flatMap(json -> {
                    RefreshSessionRecord session = fromJson(json);

                    validateSessionRecord(jwt, session, userId, instanceId, jwtAuthVersion, jwtAdminSessionVersion,
                            roles);

                    return validateCurrentUserAuthVersion(userId, jwtAuthVersion)
                            .then(validateCurrentAdminSessionVersionIfNeeded(
                                    instanceId,
                                    userId,
                                    roles,
                                    jwtAdminSessionVersion));
                });
    }

    private void validateSessionRecord(
            Jwt jwt,
            RefreshSessionRecord session,
            String userId,
            String instanceId,
            Long jwtAuthVersion,
            Long jwtAdminSessionVersion,
            List<String> roles) {
        if (!session.isActive()) {
            throw new PassportSessionValidationException("Session inactive");
        }
        if (session.getExpiresAt() == null || !session.getExpiresAt().isAfter(Instant.now())) {
            throw new PassportSessionValidationException("Session expired");
        }
        if (!Objects.equals(userId, session.getUserId())) {
            throw new PassportSessionValidationException("Session subject mismatch");
        }
        if (!Objects.equals(instanceId, session.getInstanceId())) {
            throw new PassportSessionValidationException("Session instance mismatch");
        }
        if (!Objects.equals(jwtAuthVersion, session.getAuthVersion())) {
            throw new PassportSessionValidationException("Session auth version mismatch");
        }
        if (hasRole(roles, INSTANCE_ADMIN_ROLE)) {
            if (jwtAdminSessionVersion == null) {
                throw new PassportSessionValidationException("Missing admin session version");
            }
            if (!Objects.equals(jwtAdminSessionVersion, session.getAdminSessionVersion())) {
                throw new PassportSessionValidationException("Session admin version mismatch");
            }
        }
    }

    private Mono<Void> validateCurrentUserAuthVersion(String userId, Long jwtAuthVersion) {
        return redis.opsForValue()
                .get(currentUserAuthVersionKey(userId))
                .switchIfEmpty(Mono.empty())
                .flatMap(value -> {
                    Long current = parseLong(value);

                    if (!Objects.equals(current, jwtAuthVersion)) {
                        return invalid("Current user auth version mismatch");
                    }

                    return Mono.empty();
                });
    }

    private Mono<Void> validateCurrentAdminSessionVersionIfNeeded(
            String instanceId,
            String userId,
            List<String> roles,
            Long jwtAdminSessionVersion) {
        if (!hasRole(roles, INSTANCE_ADMIN_ROLE)) {
            return Mono.empty();
        }

        return redis.opsForValue()
                .get(currentAdminSessionVersionKey(instanceId, userId))
                .switchIfEmpty(Mono.empty())
                .flatMap(value -> {
                    Long current = parseLong(value);

                    if (!Objects.equals(current, jwtAdminSessionVersion)) {
                        return invalid("Current admin session version mismatch");
                    }

                    return Mono.empty();
                });
    }

    private boolean hasRole(List<String> roles, String role) {
        return roles != null && roles.contains(role);
    }

    private Long claimAsLong(Jwt jwt, String claimName) {
        Object raw = jwt.getClaim(claimName);

        if (raw instanceof Number number) {
            return number.longValue();
        }

        if (raw instanceof String string && StringUtils.hasText(string)) {
            return parseLong(string);
        }

        return null;
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new PassportSessionValidationException("Invalid numeric claim");
        }
    }

    private RefreshSessionRecord fromJson(String json) {
        try {
            return objectMapper.readValue(json, RefreshSessionRecord.class);
        } catch (JsonProcessingException exception) {
            throw new PassportSessionValidationException("Invalid session record");
        }
    }

    private String sessionKey(String sessionId) {
        return "auth:session:" + sessionId;
    }

    private String currentUserAuthVersionKey(String userId) {
        return "auth:user-version:" + userId;
    }

    private String currentAdminSessionVersionKey(String instanceId, String userId) {
        return "auth:admin-session-version:" + instanceId + ":" + userId;
    }

    private Mono<Void> invalid(String message) {
        return Mono.error(new PassportSessionValidationException(message));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        applyCorsHeaders(exchange);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = """
                {"ok":false,"error":"UNAUTHORIZED","message":"%s"}
                """.formatted(message).getBytes(StandardCharsets.UTF_8);

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(body)));
    }

    private void applyCorsHeaders(ServerWebExchange exchange) {
        String origin = exchange.getRequest().getHeaders().getOrigin();

        if ("http://localhost:3001".equals(origin) || "http://localhost:3000".equals(origin)) {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.setAccessControlAllowOrigin(origin);
            headers.setAccessControlAllowCredentials(true);
            headers.add("Vary", "Origin");
        }
    }

    private static class PassportSessionValidationException extends RuntimeException {
        private PassportSessionValidationException(String message) {
            super(message);
        }
    }

}
