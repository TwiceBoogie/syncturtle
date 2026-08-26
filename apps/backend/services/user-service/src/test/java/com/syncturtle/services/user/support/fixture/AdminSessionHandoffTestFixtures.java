package com.syncturtle.services.user.support.fixture;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffClaim;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffReceipt;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffRecord;
import com.syncturtle.services.user.service.param.AdminSessionHandoffCreateParam;
import com.syncturtle.services.user.type.AdminSessionHandoffState;

public final class AdminSessionHandoffTestFixtures {

    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID INSTANCE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final String RECEIPT_ID = "33333333-3333-3333-3333-333333333333";
    public static final String CLAIM_ID = "44444444-4444-4444-4444-444444444444";
    public static final String OTHER_CLAIM_ID = "55555555-5555-5555-5555-555555555555";
    public static final String CODE_HASH = "a".repeat(64);
    public static final String PRE_AUTH_BINDING = "b".repeat(64);
    public static final String CLIENT_BINDING = "c".repeat(64);
    public static final Instant ISSUED_AT = Instant.parse("2026-08-21T15:00:00Z");
    public static final Instant EXPIRES_AT = ISSUED_AT.plusSeconds(30);

    private AdminSessionHandoffTestFixtures() {
    }

    public static AdminSessionHandoffRecord pendingRecord() {
        return baseRecordBuilder()
                .state(AdminSessionHandoffState.PENDING)
                .claimId(null)
                .build();
    }

    public static AdminSessionHandoffRecord claimedRecord() {
        return baseRecordBuilder()
                .state(AdminSessionHandoffState.CLAIMED)
                .claimId(CLAIM_ID)
                .build();
    }

    public static AdminSessionHandoffClaim claim() {
        return new AdminSessionHandoffClaim(RECEIPT_ID, CLAIM_ID, claimedRecord());
    }

    public static AdminSessionHandoffReceipt receipt() {
        return new AdminSessionHandoffReceipt(RECEIPT_ID + ".opaque-secret", ISSUED_AT, EXPIRES_AT);
    }

    public static AdminSessionHandoffCreateParam createParam() {
        return AdminSessionHandoffCreateParam.builder()
                .userId(USER_ID)
                .instanceId(INSTANCE_ID)
                .userAuthVersion(7L)
                .adminSessionVersion(9L)
                .preAuthBindingHash(PRE_AUTH_BINDING)
                .clientBindingHash(CLIENT_BINDING)
                .build();
    }

    public static AdminSessionHandoffRecord.AdminSessionHandoffRecordBuilder baseRecordBuilder() {
        return AdminSessionHandoffRecord.builder()
                .recordVersion(AdminSessionHandoffRecord.CURRENT_RECORD_VERSION)
                .codeHash(CODE_HASH)
                .userId(USER_ID)
                .instanceId(INSTANCE_ID)
                .userAuthVersion(7L)
                .adminSessionVersion(9L)
                .preAuthBindingHash(PRE_AUTH_BINDING)
                .clientBindingHash(CLIENT_BINDING)
                .issuedAtEpochMilli(ISSUED_AT.toEpochMilli())
                .expiresAtEpochMilli(EXPIRES_AT.toEpochMilli());
    }

}
