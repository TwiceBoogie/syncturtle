package com.syncturtle.services.user.service.collaborator.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.services.user.exception.RefreshSessionFamilySerializerException;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@DisplayName("RefreshSessionFamilySerializer")
class RefreshSessionFamilySerializerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-10T12:00:00Z");
    private static final Instant LAST_USED_AT = Instant.parse("2026-08-12T12:00:00Z");

    private RefreshSessionFamilySerializer serializer;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setup() {
        jsonMapper = JsonMapper.builder().findAndAddModules().build();
        serializer = new RefreshSessionFamilySerializer(jsonMapper);
    }

    @Nested
    @DisplayName("encode(RefreshSessionFamilyRecord)")
    class EncodeTests {

        @Test
        @DisplayName("writes strict version two wire shape")
        void writesStrictVersionTwoWireShape() throws Exception {
            // arrange
            RefreshSessionFamilyRecord record = validRecord();
            // conditions
            // act
            String result = serializer.encode(record);
            ObjectNode json = (ObjectNode) jsonMapper.readTree(result);
            // assert
            assertThat(json.get("recordVersion").intValue()).isEqualTo(2);
            assertThat(json.has("currentRefreshTokenHash")).isTrue();
            assertThat(json.has("refreshToken")).isFalse();
            assertThat(json.has("ipAddress")).isFalse();
            assertThat(json.has("userAgent")).isFalse();
            // verify
        }

    }

    @Nested
    @DisplayName("decode(String)")
    class DecodeTests {

        @Test
        @DisplayName("reads complete version two record")
        void readsCompleteVersionTwoRecord() {
            // arrange
            String json = serializer.encode(validRecord());
            // condiitons
            // act
            RefreshSessionFamilyRecord result = serializer.decode(json);
            // assert
            assertThat(result.getRecordVersion()).isEqualTo(2);
            assertThat(result.getRotationCounter()).isEqualTo(3L);
            assertThat(result.getCreatedAt()).isEqualTo(CREATED_AT);
            assertThat(result.getAbsoluteExpiresAt())
                    .isEqualTo(CREATED_AT.plus(RefreshSessionFamilyRecord.ABSOLUTE_LIFETIME));
            // verify
        }

        @Test
        @DisplayName("rejects missing record version")
        void rejectsMissingRecordVersion() throws Exception {
            // arrange
            ObjectNode json = validJson();
            json.remove("recordVersion");
            // conditions
            // act
            RefreshSessionFamilySerializerException failure = catchThrowableOfType(
                    RefreshSessionFamilySerializerException.class,
                    () -> serializer.decode(jsonMapper.writeValueAsString(json)));
            // asser
            assertThat(failure.getReason())
                    .isEqualTo(RefreshSessionFamilySerializerException.Reason.MISSING_RECORD_VERSION);
            // verify
        }

        @Test
        @DisplayName("rejects version one record")
        void rejectsVersionOneRecord() throws Exception {
            // arrange
            ObjectNode json = jsonMapper.createObjectNode();
            json.put("recordVersion", 1);
            json.put("email", "legacy@example.test");
            json.put("refreshTokenHash", "legacy-hash-shape");
            // conditions
            // act
            RefreshSessionFamilySerializerException failure = catchThrowableOfType(
                    RefreshSessionFamilySerializerException.class,
                    () -> serializer.decode(jsonMapper.writeValueAsString(json)));
            // assert
            assertThat(failure.getReason())
                    .isEqualTo(RefreshSessionFamilySerializerException.Reason.UNSUPPORTED_RECORD_VERSION);
            // verify
        }

        @Test
        @DisplayName("rejects future record version")
        void rejectsFutureRecordVersion() throws Exception {
            // arrange
            ObjectNode json = validJson();
            json.put("recordVersion", 3);
            // conditions
            // act
            RefreshSessionFamilySerializerException failure = catchThrowableOfType(
                    RefreshSessionFamilySerializerException.class,
                    () -> serializer.decode(jsonMapper.writeValueAsString(json)));
            // assert
            assertThat(failure.getReason())
                    .isEqualTo(RefreshSessionFamilySerializerException.Reason.UNSUPPORTED_RECORD_VERSION);
            // verify
        }

        @Test
        @DisplayName("rejects string record version")
        void rejectsStringRecordVersion() throws Exception {
            // arrange
            ObjectNode json = validJson();
            json.put("recordVersion", "2");
            // conditions
            // act
            RefreshSessionFamilySerializerException failure = catchThrowableOfType(
                    RefreshSessionFamilySerializerException.class,
                    () -> serializer.decode(jsonMapper.writeValueAsString(json)));
            // assert
            assertThat(failure.getReason())
                    .isEqualTo(RefreshSessionFamilySerializerException.Reason.MALFORMED_RECORD_VERSION);
            // verify
        }

        @Test
        @DisplayName("rejects long record version")
        void rejectsLongRecordVersion() throws Exception {
            // arrange
            ObjectNode json = validJson();
            json.put("recordVersion", (long) Integer.MAX_VALUE + 1L);
            // conditions
            // act
            RefreshSessionFamilySerializerException failure = catchThrowableOfType(
                    RefreshSessionFamilySerializerException.class,
                    () -> serializer.decode(jsonMapper.writeValueAsString(json)));
            // assert
            assertThat(failure.getReason())
                    .isEqualTo(RefreshSessionFamilySerializerException.Reason.MALFORMED_RECORD_VERSION);
            // verify
        }

        @Test
        @DisplayName("rejects missing required counter")
        void rejectsMissingRequiredCounter() throws Exception {
            // arrange
            ObjectNode json = validJson();
            json.remove("rotationCounter");
            // conditions
            // act
            RefreshSessionFamilySerializerException failure = catchThrowableOfType(
                    RefreshSessionFamilySerializerException.class,
                    () -> serializer.decode(jsonMapper.writeValueAsString(json)));
            // assert
            assertThat(failure.getReason())
                    .isEqualTo(RefreshSessionFamilySerializerException.Reason.MALFORMED_RECORD);
            // verify
        }

        @Test
        @DisplayName("rejects unknown legacy field")
        void rejectsUnknownLegacyField() throws Exception {
            // arrange
            ObjectNode json = validJson();
            json.put("userAgent", "legacy-value");
            // conditions
            // act
            RefreshSessionFamilySerializerException failure = catchThrowableOfType(
                    RefreshSessionFamilySerializerException.class,
                    () -> serializer.decode(jsonMapper.writeValueAsString(json)));
            // assert
            assertThat(failure.getReason())
                    .isEqualTo(RefreshSessionFamilySerializerException.Reason.MALFORMED_RECORD);
            // verify
        }

        @Test
        @DisplayName("rejects malformed json")
        void rejectsMalformedJson() {
            // arrange
            // conditions
            // act
            RefreshSessionFamilySerializerException failure = catchThrowableOfType(
                    RefreshSessionFamilySerializerException.class,
                    () -> serializer.decode("{not-json"));
            // assert
            assertThat(failure.getReason())
                    .isEqualTo(RefreshSessionFamilySerializerException.Reason.MALFORMED_RECORD);
            // verify
        }

    }

    private static RefreshSessionFamilyRecord validRecord() {
        return RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId("c12c7988-b9fe-4299-a72f-e430d004d37b")
                .instanceId("91ff9854-e8ac-4bf5-91d8-3117dcd8d05c")
                .roles(List.of("USER"))
                .authVersion(4L)
                .currentRefreshTokenHash("a".repeat(64))
                .rotationCounter(3L)
                .createdAt(CREATED_AT)
                .lastUsedAt(LAST_USED_AT)
                .idleExpiresAt(LAST_USED_AT.plus(RefreshSessionFamilyRecord.IDLE_LIFETIME))
                .absoluteExpiresAt(CREATED_AT.plus(RefreshSessionFamilyRecord.ABSOLUTE_LIFETIME))
                .deviceLabel("Firefox on macOS")
                .clientBindingHash("b".repeat(64))
                .build();
    }

    private ObjectNode validJson() throws Exception {
        return (ObjectNode) jsonMapper.readTree(serializer.encode(validRecord()));
    }
}
