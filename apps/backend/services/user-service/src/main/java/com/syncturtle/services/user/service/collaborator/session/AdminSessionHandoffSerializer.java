package com.syncturtle.services.user.service.collaborator.session;

import java.util.Map.Entry;
import java.util.Set;

import org.springframework.util.Assert;

import com.syncturtle.services.user.exception.AdminSessionHandoffException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class AdminSessionHandoffSerializer {

    private static final String RECORD_VERSION = "recordVersion";
    private static final Set<String> SUPPORTED_FIELDS = Set.of(
            RECORD_VERSION,
            "codeHash",
            "userId",
            "instanceId",
            "userAuthVersion",
            "adminSessionVersion",
            "preAuthBindingHash",
            "clientBindingHash",
            "issuedAtEpochMilli",
            "expiresAtEpochMilli",
            "state",
            "claimId");
    private static final Set<String> REQUIRED_FIELDS = SUPPORTED_FIELDS;

    private final JsonMapper jsonMapper;

    public AdminSessionHandoffSerializer(JsonMapper jsonMapper) {
        Assert.notNull(jsonMapper, "jsonMapper is required");

        this.jsonMapper = jsonMapper;
    }

    public String encode(AdminSessionHandoffRecord record) {
        Assert.notNull(record, "admin session handoff record is required");

        try {
            return jsonMapper.writeValueAsString(record);
        } catch (JacksonException exception) {
            throw malformed("failed to serialize admin session handoff record", exception);
        }
    }

    public AdminSessionHandoffRecord decode(String json) {
        if (json == null || json.isBlank()) {
            throw malformed("admin session handoff JSON is required", null);
        }

        try {
            JsonNode root = jsonMapper.readTree(json);
            requireObject(root);
            requireSupportedVersion(root);
            requireSupportedFields(root);
            requireRequiredFields(root);

            AdminSessionHandoffRecord record = jsonMapper.treeToValue(root, AdminSessionHandoffRecord.class);
            if (record == null) {
                throw malformed("admin session handoff record is required", null);
            }
            return record;
        } catch (AdminSessionHandoffException exception) {
            throw exception;
        } catch (JacksonException exception) {
            throw malformed("malformed admin session handoff record", exception);
        } catch (RuntimeException exception) {
            throw malformed("malformed admin session handoff record", exception);
        }
    }

    private static void requireObject(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw malformed("admin session handoff record must be a JSON object", null);
        }
    }

    private static void requireSupportedVersion(JsonNode root) {
        JsonNode version = root.get(RECORD_VERSION);
        if (version == null || version.isNull() || !version.isInt()) {
            throw malformed("admin session handoff recordVersion must be an integer", null);
        }
        if (version.intValue() != AdminSessionHandoffRecord.CURRENT_RECORD_VERSION) {
            throw new AdminSessionHandoffException(
                    AdminSessionHandoffException.Reason.UNSUPPORTED_RECORD_VERSION,
                    "unsupported admin session handoff recordVersion");
        }
    }

    private static void requireSupportedFields(JsonNode root) {
        for (Entry<String, JsonNode> property : root.properties()) {
            if (!SUPPORTED_FIELDS.contains(property.getKey())) {
                throw malformed("admin session handoff record contains an unsupported field", null);
            }
        }
    }

    private static void requireRequiredFields(JsonNode root) {
        for (String field : REQUIRED_FIELDS) {
            if (!root.has(field)) {
                throw malformed("admin session handoff record is missing a required field", null);
            }
            if (!"claimId".equals(field) && root.get(field).isNull()) {
                throw malformed("admin session handoff record contains a null required field", null);
            }
        }
    }

    private static AdminSessionHandoffException malformed(String message, Throwable cause) {
        if (cause == null) {
            return new AdminSessionHandoffException(
                    AdminSessionHandoffException.Reason.MALFORMED_RECORD,
                    message);
        }
        return new AdminSessionHandoffException(
                AdminSessionHandoffException.Reason.MALFORMED_RECORD,
                message,
                cause);
    }

}
