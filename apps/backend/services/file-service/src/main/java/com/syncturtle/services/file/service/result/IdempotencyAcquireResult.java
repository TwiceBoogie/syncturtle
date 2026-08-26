package com.syncturtle.services.file.service.result;

import java.util.UUID;

import lombok.Getter;

@Getter
public final class IdempotencyAcquireResult {

    public enum Decision {
        ACQUIRED,
        REPLAY,
        IN_PROGRESS,
        CONFLICT
    }

    private final Decision decision;
    private final UUID recordId;
    private final UUID processingToken;
    private final String responseBody;

    private IdempotencyAcquireResult(Decision decision, UUID recordId, UUID processingToken, String responseBody) {
        this.decision = decision;
        this.recordId = recordId;
        this.processingToken = processingToken;
        this.responseBody = responseBody;
    }

    public static IdempotencyAcquireResult acquired(UUID recordId, UUID processingToken) {
        return new IdempotencyAcquireResult(Decision.ACQUIRED, recordId, processingToken, null);
    }

    public static IdempotencyAcquireResult replay(UUID recordId, String responseBody) {
        return new IdempotencyAcquireResult(Decision.REPLAY, recordId, null, responseBody);
    }

    public static IdempotencyAcquireResult inProgress(UUID recordId) {
        return new IdempotencyAcquireResult(Decision.IN_PROGRESS, recordId, null, null);
    }

    public static IdempotencyAcquireResult conflict(UUID recordId) {
        return new IdempotencyAcquireResult(Decision.CONFLICT, recordId, null, null);
    }

}
