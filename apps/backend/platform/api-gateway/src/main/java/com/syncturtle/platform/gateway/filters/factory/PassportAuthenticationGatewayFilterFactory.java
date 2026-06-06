package com.syncturtle.platform.gateway.filters.factory;

import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_ADMIN_SESSION_VERSION;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_INSTANCE_ID;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_ISSUER;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_ROLES;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_SESSION_ID;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_USER_AUTH_VERSION;
import static com.syncturtle.common.core.header.GatewayHeaders.HDR_AUTH_USER_ID;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import com.syncturtle.platform.gateway.configurations.properties.GatewayPassportProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public final class PassportAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<PassportAuthenticationGatewayFilterFactory.Config> {

    private static final long DEFAULT_VERSION = 0L;

    private final ReactiveStringRedisTemplate redis;
    private final GatewayPassportProperties properties;

    public PassportAuthenticationGatewayFilterFactory(
            ReactiveStringRedisTemplate redis,
            GatewayPassportProperties properties) {
        super(Config.class);
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        Config effectiveConfig = Config.withDefaults(config);

        return (exchange, chain) -> currentJwtAuthentication(exchange)
                .flatMap(authentication -> authenticate(exchange, chain, authentication, effectiveConfig))
                .thenReturn(Boolean.TRUE)
                .switchIfEmpty(Mono.defer(() -> unauthorized(exchange, "Authentication required.")
                        .thenReturn(Boolean.TRUE)))
                .then();
    }

    private Mono<Void> authenticate(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            JwtAuthenticationToken authentication,
            Config config) {
        PassportClaims claims = PassportClaims.from(authentication.getToken());

        if (!claims.hasRequiredClaims(config)) {
            return unauthorized(exchange, "Invalid access token claims.");
        }

        return validateSession(claims, config)
                .flatMap(valid -> {
                    if (!valid) {
                        return unauthorized(exchange, "Session is no longer valid.");
                    }

                    ServerHttpRequest request = addAuthenticatedHeaders(exchange.getRequest(), claims);
                    ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();

                    return chain.filter(mutatedExchange);
                });
    }

    private Mono<JwtAuthenticationToken> currentJwtAuthentication(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .filter(Authentication.class::isInstance)
                .cast(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class);
    }

    private Mono<Boolean> validateSession(PassportClaims claims, Config config) {
        if (!config.isValidateSession()) {
            return Mono.just(true);
        }

        String key = sessionKey(claims.getSessionId());

        return redis.hasKey(key)
                .doOnNext(exists -> log.info("Passport Redis session check: key={}, exists={}", key, exists))
                .flatMap(exists -> {
                    if (!Boolean.TRUE.equals(exists)) {
                        return Mono.just(false);
                    }

                    return validateUserAuthVersion(claims, config);
                })
                .doOnError(
                        exception -> log.error("Passport Redis session validation failed for key={}", key, exception))
                .onErrorReturn(false);
    }

    private Mono<Boolean> validateUserAuthVersion(PassportClaims claims, Config config) {
        if (!config.isValidateUserAuthVersion()) {
            return validateAdminSessionVersion(claims, config);
        }
        if (claims.getAuthVersion() == null) {
            return Mono.just(false);
        }

        return readVersion(userVersionKey(claims.getUserId()))
                .flatMap(currentVersion -> {
                    if (!Objects.equals(currentVersion, claims.getAuthVersion())) {
                        return Mono.just(false);
                    }

                    return validateAdminSessionVersion(claims, config);
                });
    }

    private Mono<Boolean> validateAdminSessionVersion(PassportClaims claims, Config config) {
        if (!config.isValidateAdminSessionVersion()) {
            return Mono.just(true);
        }
        if (!StringUtils.hasText(claims.getInstanceId()) || claims.getAdminSessionVersion() == null) {
            return Mono.just(false);
        }

        return readVersion(adminSessionVersionKey(claims.getInstanceId(), claims.getUserId()))
                .map(currentVersion -> Objects.equals(currentVersion, claims.getAdminSessionVersion()));
    }

    private Mono<Long> readVersion(String key) {
        return redis.opsForValue()
                .get(key)
                .map(this::parseLong)
                .defaultIfEmpty(DEFAULT_VERSION);
    }

    private ServerHttpRequest addAuthenticatedHeaders(
            ServerHttpRequest request,
            PassportClaims claims) {
        return request.mutate()
                .headers(headers -> {
                    removeAuthHeaders(headers);

                    headers.set(HDR_AUTH_USER_ID, claims.getUserId());
                    headers.set(HDR_AUTH_SESSION_ID, claims.getSessionId());
                    headers.set(HDR_AUTH_INSTANCE_ID, nullToEmpty(claims.getInstanceId()));
                    headers.set(HDR_AUTH_ROLES, String.join(",", claims.getRoles()));
                    headers.set(HDR_AUTH_USER_AUTH_VERSION, longToString(claims.getAuthVersion()));
                    headers.set(HDR_AUTH_ADMIN_SESSION_VERSION, longToString(claims.getAdminSessionVersion()));
                    headers.set(HDR_AUTH_ISSUER, nullToEmpty(claims.getIssuer()));
                })
                .build();
    }

    private static void removeAuthHeaders(HttpHeaders headers) {
        headers.remove(HDR_AUTH_USER_ID);
        headers.remove(HDR_AUTH_SESSION_ID);
        headers.remove(HDR_AUTH_INSTANCE_ID);
        headers.remove(HDR_AUTH_ROLES);
        headers.remove(HDR_AUTH_USER_AUTH_VERSION);
        headers.remove(HDR_AUTH_ADMIN_SESSION_VERSION);
        headers.remove(HDR_AUTH_ISSUER);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        return writeJson(exchange, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    private Mono<Void> writeJson(
            ServerWebExchange exchange,
            HttpStatus status,
            String error,
            String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"ok":false,"error":"%s","message":"%s"}
                """.formatted(error, message);

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private String sessionKey(String sessionId) {
        return properties.getRedis().getSessionKeyPrefix() + sessionId;
    }

    private String userVersionKey(String userId) {
        return properties.getRedis().getUserVersionKeyPrefix() + userId;
    }

    private String adminSessionVersionKey(String instanceId, String userId) {
        return properties.getRedis().getAdminSessionVersionKeyPrefix()
                + instanceId
                + ":"
                + userId;
    }

    private long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_VERSION;
        }
        return Long.parseLong(value.trim());
    }

    private static String longToString(Long value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Getter
    @Setter
    public static final class Config {
        private Boolean validateSession;
        private Boolean validateUserAuthVersion;
        private Boolean validateAdminSessionVersion;
        private Boolean requireSessionId;
        private Boolean requireInstanceId;

        public static Config withDefaults(Config config) {
            Config source = config == null ? new Config() : config;
            Config copy = new Config();
            copy.validateSession = valueOrDefault(source.validateSession, true);
            copy.validateUserAuthVersion = valueOrDefault(source.validateUserAuthVersion, true);
            copy.validateAdminSessionVersion = valueOrDefault(source.validateAdminSessionVersion, false);
            copy.requireSessionId = valueOrDefault(source.requireSessionId, true);
            copy.requireInstanceId = valueOrDefault(source.requireInstanceId, true);
            return copy;
        }

        public boolean isValidateSession() {
            return Boolean.TRUE.equals(validateSession);
        }

        public boolean isValidateUserAuthVersion() {
            return Boolean.TRUE.equals(validateUserAuthVersion);
        }

        public boolean isValidateAdminSessionVersion() {
            return Boolean.TRUE.equals(validateAdminSessionVersion);
        }

        public boolean isRequireSessionId() {
            return Boolean.TRUE.equals(requireSessionId);
        }

        public boolean isRequireInstanceId() {
            return Boolean.TRUE.equals(requireInstanceId);
        }

        private static boolean valueOrDefault(Boolean value, boolean defaultValue) {
            return value == null ? defaultValue : value;
        }
    }

    @Getter
    private static final class PassportClaims {
        private final String userId;
        private final String sessionId;
        private final String instanceId;
        private final List<String> roles;
        private final Long authVersion;
        private final Long adminSessionVersion;
        private final String issuer;

        private PassportClaims(
                String userId,
                String sessionId,
                String instanceId,
                List<String> roles,
                Long authVersion,
                Long adminSessionVersion,
                String issuer) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.instanceId = instanceId;
            this.roles = roles == null ? List.of() : List.copyOf(roles);
            this.authVersion = authVersion;
            this.adminSessionVersion = adminSessionVersion;
            this.issuer = issuer;
        }

        private static PassportClaims from(Jwt jwt) {
            return new PassportClaims(
                    jwt.getSubject(),
                    jwt.getClaimAsString("sid"),
                    jwt.getClaimAsString("instance_id"),
                    roles(jwt),
                    claimAsLong(jwt, "auth_ver"),
                    claimAsLong(jwt, "admin_session_ver"),
                    jwt.getIssuer() == null ? "" : jwt.getIssuer().toString());
        }

        private boolean hasRequiredClaims(Config config) {
            if (!StringUtils.hasText(userId)) {
                return false;
            }
            if (config.isRequireSessionId() && !StringUtils.hasText(sessionId)) {
                return false;
            }
            if (config.isRequireInstanceId() && !StringUtils.hasText(instanceId)) {
                return false;
            }
            return true;
        }

        private static List<String> roles(Jwt jwt) {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return List.of();
            }
            return List.copyOf(roles);
        }

        private static Long claimAsLong(Jwt jwt, String claimName) {
            Object raw = jwt.getClaim(claimName);
            if (raw instanceof Number number) {
                return number.longValue();
            }
            if (raw instanceof String string && !string.isBlank()) {
                return Long.parseLong(string);
            }
            return null;
        }

    }
}