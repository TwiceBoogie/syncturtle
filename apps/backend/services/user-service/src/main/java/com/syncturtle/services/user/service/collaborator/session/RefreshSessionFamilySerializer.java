package com.syncturtle.services.user.service.collaborator.session;

import static com.syncturtle.services.user.exception.RefreshSessionFamilySerializerException.Reason.MALFORMED_RECORD;
import static com.syncturtle.services.user.exception.RefreshSessionFamilySerializerException.Reason.MALFORMED_RECORD_VERSION;
import static com.syncturtle.services.user.exception.RefreshSessionFamilySerializerException.Reason.MISSING_RECORD_VERSION;
import static com.syncturtle.services.user.exception.RefreshSessionFamilySerializerException.Reason.UNSUPPORTED_RECORD_VERSION;

import java.util.Map.Entry;
import java.util.Set;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.services.user.exception.RefreshSessionFamilySerializerException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class RefreshSessionFamilySerializer {

    private static final String RECORD_VERSION = "recordVersion";
    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            RECORD_VERSION,
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
    private static final Set<String> REQUIRED_FIELDS = Set.of(
            RECORD_VERSION,
            "userId",
            "instanceId",
            "roles",
            "authVersion",
            "currentRefreshTokenHash",
            "rotationCounter",
            "createdAt",
            "lastUsedAt",
            "idleExpiresAt",
            "absoluteExpiresAt",
            "clientBindingHash");

    private final JsonMapper jsonMapper;

    public RefreshSessionFamilySerializer(JsonMapper jsonMapper) {
        Assert.notNull(jsonMapper, "jsonMapper is required");

        this.jsonMapper = jsonMapper;
    }

    public String encode(RefreshSessionFamilyRecord record) {
        Assert.notNull(record, "refresh session family record is required");

        try {
            return jsonMapper.writeValueAsString(record);
        } catch (JacksonException exception) {
            throw new RefreshSessionFamilySerializerException(MALFORMED_RECORD,
                    "failed to serialize refresh session family record", exception);
        }
    }

    public RefreshSessionFamilyRecord decode(String json) {
        if (json == null || json.isBlank()) {
            throw failure(MALFORMED_RECORD, "refresh session family JSON is required");
        }

        try {
            JsonNode root = jsonMapper.readTree(json);
            requireObject(root);
            requireSupportedVersion(root);
            requireSupportedFields(root);
            requireRequiredFields(root);

            RefreshSessionFamilyRecord record = jsonMapper.treeToValue(root, RefreshSessionFamilyRecord.class);
            if (record == null) {
                throw failure(MALFORMED_RECORD, "refresh session family record is required");
            }
            return record;
        } catch (RefreshSessionFamilySerializerException exception) {
            throw exception;
        } catch (JacksonException exception) {
            throw new RefreshSessionFamilySerializerException(
                    MALFORMED_RECORD,
                    "malformed refresh session family record",
                    exception);
        } catch (RuntimeException exception) {
            throw new RefreshSessionFamilySerializerException(
                    MALFORMED_RECORD,
                    "malformed refresh session family record",
                    exception);
        }
    }

    private static void requireRequiredFields(JsonNode root) {
        for (String field : REQUIRED_FIELDS) {
            if (!root.has(field) || root.get(field).isNull()) {
                throw failure(MALFORMED_RECORD, "refresh session family record is missing a required field");
            }
        }
    }

    private static void requireObject(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw failure(MALFORMED_RECORD, "refresh session family record must be a JSON object");
        }
    }

    private static void requireSupportedFields(JsonNode root) {
        for (Entry<String, JsonNode> property : root.properties()) {
            if (!SUPPORTED_FIELDS.contains(property.getKey())) {
                throw failure(MALFORMED_RECORD, "refresh session family record contains an unsupported field");
            }
        }
    }

    private static void requireSupportedVersion(JsonNode root) {
        JsonNode version = root.get(RECORD_VERSION);
        if (version == null || version.isNull()) {
            throw failure(MISSING_RECORD_VERSION, "refresh session family recordVersion is required");
        }
        if (!version.isInt()) {
            throw failure(MALFORMED_RECORD_VERSION, "refresh session family recordVersion must be an integer");
        }
        if (version.intValue() != RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION) {
            throw failure(UNSUPPORTED_RECORD_VERSION, "unsupported refresh session family recordVersion");
        }
    }

    private static RefreshSessionFamilySerializerException failure(
            RefreshSessionFamilySerializerException.Reason reason, String message) {
        return new RefreshSessionFamilySerializerException(reason, message);
    }

}
