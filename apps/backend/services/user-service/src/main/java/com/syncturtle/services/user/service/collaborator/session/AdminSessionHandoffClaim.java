package com.syncturtle.services.user.service.collaborator.session;

import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.services.user.type.AdminSessionHandoffState;

import lombok.Getter;

@Getter
public final class AdminSessionHandoffClaim {

    private final String receiptId;
    private final String claimId;
    private final AdminSessionHandoffRecord record;

    public AdminSessionHandoffClaim(String receiptId, String claimId, AdminSessionHandoffRecord record) {
        String canonicalReceiptId = requireCanonicalUuid(receiptId, "receiptId");
        String canonicalClaimId = requireCanonicalUuid(claimId, "claimId");
        Assert.notNull(record, "record is required");
        Assert.isTrue(record.getState() == AdminSessionHandoffState.CLAIMED,
                "record must be claimed");
        Assert.isTrue(canonicalClaimId.equals(record.getClaimId()),
                "record claimId must match the claim");

        this.receiptId = canonicalReceiptId;
        this.claimId = canonicalClaimId;
        this.record = record;
    }

    private static String requireCanonicalUuid(String value, String name) {
        Assert.hasText(value, name + " is required");

        String normalized = value.trim();
        String canonical = UUID.fromString(normalized).toString();
        Assert.isTrue(canonical.equals(normalized), name + " must be a canonical UUID");
        return canonical;
    }

}
