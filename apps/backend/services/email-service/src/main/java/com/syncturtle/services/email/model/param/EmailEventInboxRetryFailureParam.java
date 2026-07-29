package com.syncturtle.services.email.model.param;

import java.time.Duration;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class EmailEventInboxRetryFailureParam {

    private static final int MAX_ERROR_LENGTH = 4_000;

    private final int maxAttempts;
    private final Duration retryDelay;
    private final String errorMessage;

    public EmailEventInboxRetryFailureParam(int maxAttempts, Duration retryDelay, String errorMessage) {
        Assert.isTrue(maxAttempts >= 1, "maxAttempts must be at least 1");
        Assert.notNull(retryDelay, "retryDelay is required");
        Assert.isTrue(!retryDelay.isNegative() && !retryDelay.isZero(), "retryDelay must be positive");
        Assert.hasText(errorMessage, "errorMessage is required");
        Assert.isTrue(errorMessage.length() <= MAX_ERROR_LENGTH, "errorMessage must be 4000 characters or fewer");

        this.maxAttempts = maxAttempts;
        this.retryDelay = retryDelay;
        this.errorMessage = errorMessage;
    }

}
