package com.syncturtle.services.user.service.collaborator.session;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;

import lombok.Getter;

@Getter
public final class RefreshSessionSnapshot {

    private final String sessionId;
    private final String exactJson;
    private final RefreshSessionRecord record;

    public RefreshSessionSnapshot(String sessionId, String exactJson, RefreshSessionRecord record) {
        Assert.hasText(sessionId, "sessionId is required");
        Assert.hasText(exactJson, "exactJson is required");
        Assert.notNull(record, "record is required");

        this.sessionId = sessionId;
        this.exactJson = exactJson;
        this.record = record;
    }

}
