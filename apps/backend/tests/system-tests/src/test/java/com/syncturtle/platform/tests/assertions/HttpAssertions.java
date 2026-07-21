package com.syncturtle.platform.tests.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Objects;

public final class HttpAssertions {

    private HttpAssertions() {
        throw new AssertionError(
                "HttpAssertions must not be instantiated");
    }

    public static void assertStatusIn(
            int actualStatus,
            int... allowedStatuses) {
        Objects.requireNonNull(
                allowedStatuses,
                "allowedStatuses must not be null");

        if (allowedStatuses.length == 0) {
            throw new IllegalArgumentException(
                    "At least one allowed HTTP status is required");
        }

        assertThat(allowedStatuses)
                .as(
                        "Expected HTTP status to be one of %s, but got %d",
                        Arrays.toString(allowedStatuses),
                        actualStatus)
                .contains(actualStatus);
    }
}