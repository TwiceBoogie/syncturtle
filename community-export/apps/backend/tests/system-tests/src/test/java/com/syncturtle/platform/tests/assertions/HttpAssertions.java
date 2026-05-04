package com.syncturtle.platform.tests.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class HttpAssertions {

    private HttpAssertions() {
    }

    @SuppressWarnings("unused")
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void assertStatusIn(int actualStatus, int... allowed) {
        Set<Integer> allowedSet = Set.of(Arrays.stream(allowed).boxed().toArray(Integer[]::new));

        assertThat(allowedSet).as("Expected HTTP status in %s but got %d", allowedSet, actualStatus)
                .contains(actualStatus);
    }

}
