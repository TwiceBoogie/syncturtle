package com.syncturtle.platform.gateway.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

import lombok.Getter;

@Getter
public final class PassportClaims {

    private static final Duration MAX_FUTURE_ISSUED_AT_SKEW = Duration.ofMinutes(1);

    private final String userId;
    private final String sessionId;
    private final String instanceId;
    private final List<String> roles;
    private final long userAuthVersion;
    private final Long adminSessionVersion;
    private final String issuer;
    private final Instant issuedAt;
    private final Instant expiresAt;

    private PassportClaims(
            String userId,
            String sessionId,
            String instanceId,
            List<String> roles,
            long userAuthVersion,
            Long adminSessionVersion,
            String issuer,
            Instant issuedAt,
            Instant expiresAt) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.instanceId = instanceId;
        this.roles = List.copyOf(roles);
        this.userAuthVersion = userAuthVersion;
        this.adminSessionVersion = adminSessionVersion;
        this.issuer = issuer;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static PassportClaims from(Jwt jwt, Clock clock) {
        Assert.notNull(jwt, "jwt is required");
        Assert.notNull(clock, "clock is required");

        try {
            String userId = requireUuid(jwt.getSubject());
            String sessionId = requireUuid(jwt.getClaimAsString("sid"));
            String instanceId = requireUuid(jwt.getClaimAsString("instance_id"));
            List<String> roles = requireRoles(jwt.getClaimAsStringList("roles"));
            long userAuthVersion = requireVersion(jwt.getClaim("auth_ver"));
            Long adminSessionVersion = optionalVersion(jwt.getClaim("admin_session_ver"));
            String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
            Instant issuedAt = jwt.getIssuedAt();
            Instant expiresAt = jwt.getExpiresAt();

            if (!StringUtils.hasText(issuer) || issuedAt == null || expiresAt == null) {
                throw invalidClaims();
            }
            if (issuedAt.isAfter(Instant.now(clock).plus(MAX_FUTURE_ISSUED_AT_SKEW))) {
                throw invalidClaims();
            }

            return new PassportClaims(
                    userId,
                    sessionId,
                    instanceId,
                    roles,
                    userAuthVersion,
                    adminSessionVersion,
                    issuer,
                    issuedAt,
                    expiresAt);
        } catch (PassportAuthenticationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PassportAuthenticationException(
                    PassportAuthenticationFailureReason.JWT_CLAIMS_INVALID,
                    exception);
        }
    }

    public boolean isInstanceAdmin() {
        return roles.contains("INSTANCE_ADMIN");
    }

    private static String requireUuid(String value) {
        if (!StringUtils.hasText(value)) {
            throw invalidClaims();
        }

        String normalized = value.trim();
        UUID.fromString(normalized);
        return normalized;
    }

    private static List<String> requireRoles(List<String> rawRoles) {
        if (rawRoles == null || rawRoles.isEmpty()) {
            throw invalidClaims();
        }

        List<String> roles = new ArrayList<>(rawRoles.size());
        Set<String> distinct = new HashSet<>();

        for (String rawRole : rawRoles) {
            if (!StringUtils.hasText(rawRole)) {
                throw invalidClaims();
            }

            String role = rawRole.trim();
            if (!distinct.add(role)) {
                throw invalidClaims();
            }
            roles.add(role);
        }

        Collections.sort(roles);
        return roles;
    }

    private static long requireVersion(Object rawValue) {
        Long version = optionalVersion(rawValue);
        if (version == null) {
            throw invalidClaims();
        }
        return version;
    }

    private static Long optionalVersion(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        long value;
        if (rawValue instanceof Number number) {
            value = number.longValue();
        } else if (rawValue instanceof String string && StringUtils.hasText(string)) {
            value = Long.parseLong(string.trim());
        } else {
            throw invalidClaims();
        }

        if (value < 0) {
            throw invalidClaims();
        }
        return value;
    }

    private static PassportAuthenticationException invalidClaims() {
        return new PassportAuthenticationException(PassportAuthenticationFailureReason.JWT_CLAIMS_INVALID);
    }
}
