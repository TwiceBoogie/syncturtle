package com.syncturtle.services.user.service.collaborator.session;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.services.user.type.RefreshSessionLifecycleOutcome;

import lombok.Getter;

@Getter
public final class RefreshSessionLifecycleResult {

    private final RefreshSessionLifecycleOutcome outcome;
    private final String sessionId;
    private final String refreshToken;
    private final RefreshSessionFamilyRecord family;

    public RefreshSessionLifecycleResult(
            RefreshSessionLifecycleOutcome outcome,
            String sessionId,
            String refreshToken,
            RefreshSessionFamilyRecord family) {
        Assert.notNull(outcome, "outcome is required");
        Assert.hasText(sessionId, "sessionId is required");
        Assert.hasText(refreshToken, "refreshToken is required");
        Assert.notNull(family, "family is required");
        Assert.isTrue(refreshToken.startsWith(sessionId + "."),
                "refreshToken must belong to sessionId");

        this.outcome = outcome;
        this.sessionId = sessionId;
        this.refreshToken = refreshToken;
        this.family = family;
    }

}
