package com.syncturtle.services.user.service.collaborator.session;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

import com.syncturtle.services.user.type.AdminSessionHandoffState;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class AdminSessionHandoffRecord {

    public static final int CURRENT_RECORD_VERSION = 1;
    private static final long MAXIMUM_LIFETIME_MILLIS = 30_000L;
    private static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");

    private final int recordVersion;
    private final String codeHash;
    private final UUID userId;
    private final UUID instanceId;
    private final long userAuthVersion;
    private final long adminSessionVersion;
    private final String preAuthBindingHash;
    private final String clientBindingHash;
    private final long issuedAtEpochMilli;
    private final long expiresAtEpochMilli;
    private final AdminSessionHandoffState state;
    private final String claimId;

    @Builder
    @Jacksonized
    private AdminSessionHandoffRecord(
            int recordVersion,
            String codeHash,
            UUID userId,
            UUID instanceId,
            long userAuthVersion,
            long adminSessionVersion,
            String preAuthBindingHash,
            String clientBindingHash,
            long issuedAtEpochMilli,
            long expiresAtEpochMilli,
            AdminSessionHandoffState state,
            String claimId) {
        Assert.isTrue(recordVersion == CURRENT_RECORD_VERSION, "unsupported admin handoff recordVersion");
        requireHash(codeHash, "codeHash");
        Assert.notNull(userId, "userId is required");
        Assert.notNull(instanceId, "instanceId is required");
        Assert.isTrue(userAuthVersion >= 0, "userAuthVersion must not be negative");
        Assert.isTrue(adminSessionVersion >= 0, "adminSessionVersion must not be negative");
        requireHash(preAuthBindingHash, "preAuthBindingHash");
        requireHash(clientBindingHash, "clientBindingHash");
        Assert.isTrue(issuedAtEpochMilli >= 0, "issuedAtEpochMilli must not be negative");
        Assert.isTrue(expiresAtEpochMilli > issuedAtEpochMilli,
                "expiresAtEpochMilli must be after issuedAtEpochMilli");
        Assert.isTrue(expiresAtEpochMilli - issuedAtEpochMilli <= MAXIMUM_LIFETIME_MILLIS,
                "handoff lifetime must not exceed 30 seconds");
        Assert.notNull(state, "state is required");

        boolean claimed = state == AdminSessionHandoffState.CLAIMED;
        Assert.isTrue(claimed == (claimId != null), "claimId must exist exactly when state is CLAIMED");
        if (claimId != null) {
            requireCanonicalUuid(claimId, "claimId");
        }

        this.recordVersion = recordVersion;
        this.codeHash = codeHash;
        this.userId = userId;
        this.instanceId = instanceId;
        this.userAuthVersion = userAuthVersion;
        this.adminSessionVersion = adminSessionVersion;
        this.preAuthBindingHash = preAuthBindingHash;
        this.clientBindingHash = clientBindingHash;
        this.issuedAtEpochMilli = issuedAtEpochMilli;
        this.expiresAtEpochMilli = expiresAtEpochMilli;
        this.state = state;
        this.claimId = claimId;
    }

    private static void requireHash(String value, String name) {
        Assert.hasText(value, name + " is required");
        Assert.isTrue(HASH.matcher(value).matches(), name + " must be a lowercase SHA-256 value");
    }

    private static void requireCanonicalUuid(String value, String name) {
        Assert.hasText(value, name + " is required");

        String normalized = value.trim();
        String canonical = UUID.fromString(normalized).toString();
        Assert.isTrue(canonical.equals(normalized), name + " must be a canonical UUID");
    }

}
