package com.syncturtle.platform.gateway.security.session;

import static com.syncturtle.platform.gateway.support.fixture.RefreshSessionFamilyRecordFixtures.INSTANCE_ID;
import static com.syncturtle.platform.gateway.support.fixture.RefreshSessionFamilyRecordFixtures.NOW;
import static com.syncturtle.platform.gateway.support.fixture.RefreshSessionFamilyRecordFixtures.USER_ID;
import static com.syncturtle.platform.gateway.support.fixture.RefreshSessionFamilyRecordFixtures.absoluteExpiredRecord;
import static com.syncturtle.platform.gateway.support.fixture.RefreshSessionFamilyRecordFixtures.idleExpiredRecord;
import static com.syncturtle.platform.gateway.support.fixture.RefreshSessionFamilyRecordFixtures.validAdminRecord;
import static com.syncturtle.platform.gateway.support.fixture.RefreshSessionFamilyRecordFixtures.validRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.contracts.auth.session.RefreshSessionFamilyRecord;
import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@DisplayName("PassportSessionRecordParser")
class PassportSessionRecordParserTest {

    private static final String SESSION_ID = "3a428175-bef1-4c13-ae53-997b6fbfc508";

    private JsonMapper jsonMapper;
    private PassportSessionRecordParser parser;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder().findAndAddModules().build();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        parser = new PassportSessionRecordParser(jsonMapper, clock);
    }

    @Nested
    @DisplayName("parse(String,String)")
    class Parse {

        @Test
        @DisplayName("parses complete supported record")
        void parsesCompleteSupportedRecord() throws Exception {
            RefreshSessionFamilyRecord record = validRecord();
            String json = jsonMapper.writeValueAsString(record);

            ParsedPassportSession result = parser.parse(SESSION_ID, json);

            assertThat(result.getRecordVersion()).isEqualTo(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION);
            assertThat(result.getSessionId()).isEqualTo(SESSION_ID);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getInstanceId()).isEqualTo(INSTANCE_ID);
            assertThat(result.getRoles()).containsExactly("USER");
            assertThat(result.getUserAuthVersion()).isEqualTo(4L);
        }

        @Test
        @DisplayName("rejects malformed json")
        void rejectsMalformedJson() {
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> parser.parse(SESSION_ID, "{not-json"));

            assertThat(failure.getReason()).isEqualTo(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }

        @Test
        @DisplayName("rejects missing record version")
        void rejectsMissingRecordVersion() throws Exception {
            ObjectNode json = jsonMapper.valueToTree(validRecord());
            json.remove("recordVersion");
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> parser.parse(SESSION_ID, jsonMapper.writeValueAsString(json)));
            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }

        @Test
        @DisplayName("rejects malformed record version")
        void rejectsMalformedRecordVersion() throws Exception {
            ObjectNode json = jsonMapper.valueToTree(validRecord());
            json.put("recordVersion", "not-a-version");
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> parser.parse(SESSION_ID, jsonMapper.writeValueAsString(json)));
            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }

        @Test
        @DisplayName("rejects unsupported wire field")
        void rejectsUnsupportedWireField() throws Exception {
            ObjectNode json = jsonMapper.valueToTree(validRecord());
            json.put("legacyActive", true);

            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> parser.parse(SESSION_ID, jsonMapper.writeValueAsString(json)));

            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }

        @Test
        @DisplayName("rejects unsupported record version")
        void rejectsUnsupportedRecordVersion() throws Exception {
            ObjectNode json = jsonMapper.valueToTree(validRecord());
            json.put("recordVersion", RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION + 1);
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> parser.parse(SESSION_ID, jsonMapper.writeValueAsString(json)));
            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.SESSION_VERSION_UNSUPPORTED);
        }

        @Test
        @DisplayName("rejects v1 record version")
        void rejectsV1RecordVersion() throws Exception {
            ObjectNode json = jsonMapper.valueToTree(validRecord());
            json.put("recordVersion", 1);
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> parser.parse(SESSION_ID, jsonMapper.writeValueAsString(json)));
            assertThat(failure.getReason())
                    .isEqualTo(PassportAuthenticationFailureReason.SESSION_VERSION_UNSUPPORTED);
        }

        @Test
        @DisplayName("rejects idle expired record")
        void rejectsIdleExpiredRecord() throws Exception {
            RefreshSessionFamilyRecord record = idleExpiredRecord();
            String json = jsonMapper.writeValueAsString(record);
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> parser.parse(SESSION_ID, json));
            assertThat(failure.getReason()).isEqualTo(PassportAuthenticationFailureReason.SESSION_EXPIRED);
        }

        @Test
        @DisplayName("rejects absolute expired record")
        void rejectsAbsoluteExpiredRecord() throws Exception {
            RefreshSessionFamilyRecord record = absoluteExpiredRecord();
            String json = jsonMapper.writeValueAsString(record);
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> parser.parse(SESSION_ID, json));
            assertThat(failure.getReason()).isEqualTo(PassportAuthenticationFailureReason.SESSION_EXPIRED);
        }

        @Test
        @DisplayName("rejects admin role without admin version")
        void rejectsAdminRoleWithoutAdminVersion() throws Exception {
            ObjectNode json = jsonMapper.valueToTree(validAdminRecord());
            json.remove("adminSessionVersion");
            PassportAuthenticationException failure = catchThrowableOfType(
                    PassportAuthenticationException.class,
                    () -> parser.parse(SESSION_ID, jsonMapper.writeValueAsString(json)));
            assertThat(failure.getReason()).isEqualTo(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }
    }

}
