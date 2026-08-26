package com.syncturtle.common.web.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

@DisplayName("CursorCodec")
class CursorCodecTest {

    CursorCodec codec;

    @BeforeEach
    void setup() {
        codec = new CursorCodec(new JsonMapper());
    }

    @Nested
    @DisplayName("encode(UUID, Instant)")
    class EncodeTests {

        @Test
        @DisplayName("rejects null id")
        void rejectsNullId() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> codec.encode(null, Instant.parse("2026-01-01T00:00:00Z")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("id is required");
            // verify
        }

        @Test
        @DisplayName("rejects null createdAt")
        void rejectsNullCreatedAt() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> codec.encode(UUID.randomUUID(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("createdAt is required");
            // verify
        }

        @Test
        @DisplayName("encodes cursor as url safe base64 without padding")
        void encodesCursorAsUrlSafeBase64WithoutPadding() {
            // arrange
            UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
            // conditions
            // act
            String actual = codec.encode(id, createdAt);
            // assert
            assertThat(actual).isNotBlank();
            assertThat(actual).doesNotContain("+", "/", "=");
        }

    }

    @Nested
    @DisplayName("decode(String)")
    class DecodeTests {

        @Test
        @DisplayName("returns null for null cursor")
        void returnsNullForNullCursor() {
            assertThat(codec.decode(null)).isNull();
        }

        @Test
        @DisplayName("returns null for blank cursor")
        void returnsNullForBlankCursor() {
            assertThat(codec.decode(" ")).isNull();
        }

        @Test
        @DisplayName("round trips encoded cursor")
        void roundTripsEncodedCursor() {
            // arrange
            UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
            Instant createdAt = Instant.parse("2026-01-01T12:30:00Z");
            String encoded = codec.encode(id, createdAt);
            // conditions
            // act
            DecodedCursor actual = codec.decode(encoded);
            // assert
            assertThat(actual).isNotNull();
            assertThat(actual.getId()).isEqualTo(id);
            assertThat(actual.getCreatedAt()).isEqualTo(createdAt);
            // verify
        }

        @Test
        @DisplayName("decodes cursor even when padding is missing")
        void decodesCursorEvenWhenPaddingIsMissing() {
            // arrange
            UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
            Instant createdAt = Instant.parse("2026-01-01T12:30:00Z");

            String encoded = codec.encode(id, createdAt);
            String withoutPadding = encoded.replace("=", "");
            // conditions
            // act
            DecodedCursor actual = codec.decode(withoutPadding);
            // assert
            assertThat(actual).isNotNull();
            assertThat(actual.getId()).isEqualTo(id);
            assertThat(actual.getCreatedAt()).isEqualTo(createdAt);
            // verify
        }

        @Test
        @DisplayName("returns null for invalid base64")
        void returnsNullForInvalidBase64() {
            // arrange
            // conditions
            // act
            DecodedCursor actual = codec.decode("not-valid-base64%%%");
            // assert
            assertThat(actual).isNull();
            // verify
        }

        @Test
        @DisplayName("returns null when json is missing required fields")
        void returnsNullWhenJsonIsMissingRequiredFields() {
            // arrange
            String missingFields = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            "{\"id\":\"44444444-4444-4444-4444-444444444444\"}".getBytes(StandardCharsets.UTF_8));
            // conditions
            // act
            DecodedCursor actual = codec.decode(missingFields);
            // assert
            assertThat(actual).isNull();
            // verify
        }

        @Test
        @DisplayName("returns null when createdAt is invalid")
        void returnsNullWhenCreatedAtIsInvalid() {
            // arrange
            String invalidCreatedAt = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(("""
                            {
                                "id": "55555555-5555-5555-5555-555555555555",
                                "createdAt": "not-an-instant"
                            }
                            """).getBytes(StandardCharsets.UTF_8));
            // conditions
            // act
            DecodedCursor actual = codec.decode(invalidCreatedAt);
            // assert
            assertThat(actual).isNull();
            // verify
        }

        @Test
        @DisplayName("returns null when id is invalid")
        void returnsNullWhenIdIsInvalid() {
            // arrange
            String invalidId = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(("""
                            {
                                "id": "not-a-uuid",
                                "createdAt": "2026-01-01T00:00:00Z"
                            }
                            """).getBytes(StandardCharsets.UTF_8));
            // conditions
            // act
            DecodedCursor actual = codec.decode(invalidId);
            // assert
            assertThat(actual).isNull();
            // verify
        }

    }

}
