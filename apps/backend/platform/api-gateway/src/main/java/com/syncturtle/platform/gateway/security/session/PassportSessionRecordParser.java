package com.syncturtle.platform.gateway.security.session;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public final class PassportSessionRecordParser {

    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            "recordVersion",
            "userId",
            "instanceId",
            "roles",
            "authVersion",
            "adminSessionVersion",
            "currentRefreshTokenHash",
            "rotationCounter",
            "createdAt",
            "lastUsedAt",
            "idleExpiresAt",
            "absoluteExpiresAt",
            "deviceLabel",
            "clientBindingHash");

    private final JsonMapper jsonMapper;
    private final Clock clock;

    public ParsedPassportSession parse(String sessionId, String json) {
        if (!StringUtils.hasText(json)) {
            throw failure(PassportAuthenticationFailureReason.SESSION_MISSING);
        }

        RefreshSessionFamilyRecord record;
        try {
            JsonNode root = jsonMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
            }

            JsonNode recordVersion = root.get("recordVersion");
            if (recordVersion == null || !recordVersion.isInt()) {
                throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
            }
            if (recordVersion.intValue() != RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION) {
                throw failure(PassportAuthenticationFailureReason.SESSION_VERSION_UNSUPPORTED);
            }
            requireSupportedFields(root);

            record = jsonMapper.treeToValue(root, RefreshSessionFamilyRecord.class);
        } catch (PassportAuthenticationException exception) {
            throw exception;
        } catch (JacksonException exception) {
            throw new PassportAuthenticationException(PassportAuthenticationFailureReason.SESSION_MALFORMED, exception);
        } catch (RuntimeException exception) {
            throw new PassportAuthenticationException(PassportAuthenticationFailureReason.SESSION_MALFORMED, exception);
        }

        if (record == null) {
            throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }

        try {
            String parsedSessionId = requireUuid(sessionId);
            String userId = requireUuid(record.getUserId());
            String instanceId = requireUuid(record.getInstanceId());
            List<String> roles = requireRoles(record.getRoles());
            long userAuthVersion = requireVersion(record.getAuthVersion());
            Long adminSessionVersion = optionalVersion(record.getAdminSessionVersion());
            Instant issuedAt = record.getCreatedAt();
            Instant expiresAt = record.getIdleExpiresAt();
            Instant absoluteExpiresAt = record.getAbsoluteExpiresAt();

            if (issuedAt == null || expiresAt == null || absoluteExpiresAt == null) {
                throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
            }
            if (roles.contains("INSTANCE_ADMIN") != (adminSessionVersion != null)) {
                throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
            }
            Instant now = Instant.now(clock);
            if (!expiresAt.isAfter(now) || !absoluteExpiresAt.isAfter(now)) {
                throw failure(PassportAuthenticationFailureReason.SESSION_EXPIRED);
            }

            return new ParsedPassportSession(
                    record.getRecordVersion(),
                    parsedSessionId,
                    userId,
                    instanceId,
                    roles,
                    userAuthVersion,
                    adminSessionVersion,
                    true,
                    issuedAt,
                    expiresAt);
        } catch (PassportAuthenticationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PassportAuthenticationException(PassportAuthenticationFailureReason.SESSION_MALFORMED, exception);
        }
    }

    private static void requireSupportedFields(JsonNode root) {
        for (Entry<String, JsonNode> property : root.properties()) {
            if (!SUPPORTED_FIELDS.contains(property.getKey())) {
                throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
            }
        }
    }

    private static String requireUuid(String value) {
        if (!StringUtils.hasText(value)) {
            throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }
        String normalized = value.trim();
        UUID.fromString(normalized);
        return normalized;
    }

    private static List<String> requireRoles(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }

        List<String> roles = new ArrayList<>(values.size());
        Set<String> distinct = new HashSet<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
            }
            String role = value.trim();
            if (!distinct.add(role)) {
                throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
            }
            roles.add(role);
        }
        Collections.sort(roles);
        return roles;
    }

    private static long requireVersion(Long value) {
        Long version = optionalVersion(value);
        if (version == null) {
            throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }
        return version;
    }

    private static Long optionalVersion(Long value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw failure(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }
        return value;
    }

    private static PassportAuthenticationException failure(PassportAuthenticationFailureReason reason) {
        return new PassportAuthenticationException(reason);
    }

}
