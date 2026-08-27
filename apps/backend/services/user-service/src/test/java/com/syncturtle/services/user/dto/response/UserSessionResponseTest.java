package com.syncturtle.services.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class UserSessionResponseTest {

    private static final Instant NOW = Instant.parse("2026-08-25T15:00:00Z");

    @Nested
    class Serialization {

        @Test
        void exposesExactlyTheSevenApprovedSessionFields() throws Exception {
            // arrange
            UserSessionResponse session = session();
            JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
            // conditions
            // act
            JsonNode json = mapper.readTree(mapper.writeValueAsString(session));
            // assert
            assertThat(json.size()).isEqualTo(7);
            assertThat(json.has("sessionId")).isTrue();
            assertThat(json.has("current")).isTrue();
            assertThat(json.has("administrator")).isTrue();
            assertThat(json.has("createdAt")).isTrue();
            assertThat(json.has("lastUsedAt")).isTrue();
            assertThat(json.has("idleExpiresAt")).isTrue();
            assertThat(json.has("absoluteExpiresAt")).isTrue();
            assertThat(json.has("deviceLabel")).isFalse();
            assertThat(json.has("currentRefreshTokenHash")).isFalse();
            assertThat(json.has("authVersion")).isFalse();
            assertThat(json.has("adminSessionVersion")).isFalse();
            assertThat(json.has("instanceId")).isFalse();
            assertThat(json.has("roles")).isFalse();
        }

    }

    @Nested
    class Construction {

        @Test
        void defensivelyCopiesSessionCollection() {
            // arrange
            List<UserSessionResponse> mutable = new ArrayList<>();
            mutable.add(session());
            UserSessionInventoryResponse response = new UserSessionInventoryResponse(mutable);
            // act
            mutable.clear();
            // assert
            assertThat(response.getSessions()).hasSize(1);
            assertThatThrownBy(() -> response.getSessions().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    private static UserSessionResponse session() {
        return UserSessionResponse.builder()
                .sessionId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .current(true)
                .administrator(false)
                .createdAt(NOW.minusSeconds(3600))
                .lastUsedAt(NOW)
                .idleExpiresAt(NOW.plusSeconds(3600))
                .absoluteExpiresAt(NOW.plusSeconds(7200))
                .build();
    }

}
