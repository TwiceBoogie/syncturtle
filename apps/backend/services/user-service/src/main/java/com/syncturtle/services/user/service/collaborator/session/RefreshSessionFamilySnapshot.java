package com.syncturtle.services.user.service.collaborator.session;

import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;

import lombok.Getter;

@Getter
public final class RefreshSessionFamilySnapshot {

    private final UUID sessionId;
    private final RefreshSessionFamilyRecord family;

    public RefreshSessionFamilySnapshot(UUID sessionId, RefreshSessionFamilyRecord family) {
        Assert.notNull(sessionId, "sessionId is required");
        Assert.notNull(family, "family is required");

        this.sessionId = sessionId;
        this.family = family;
    }

}
