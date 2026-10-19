package com.syncturtle.platform.gateway.security.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.contracts.auth.session.RefreshSessionRecord;
import com.syncturtle.platform.gateway.exception.PassportAuthenticationException;
import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@DisplayName("PassportSessionRecordParser")
class PassportSessionRecordParserTest {

    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");
    private static final String SESSION = "3a428175-bef1-4c13-ae53-997b6fbfc508";
    private static final String USER = "c12c7988-b9fe-4299-a72f-e430d004d37b";
    private static final String INSTANCE = "91ff9854-e8ac-4bf5-91d8-3117dcd8d05c";

    private JsonMapper mapper;
    private PassportSessionRecordParser parser;

    @BeforeEach
    void setUp() {
        mapper = JsonMapper.builder().findAndAddModules().build();
        parser = new PassportSessionRecordParser(mapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("parse(String,String)")
    class Parse {

        @Test
        void acceptsCompleteCurrentRecord() throws Exception {
            ParsedPassportSession parsed = parser.parse(SESSION, mapper.writeValueAsString(record()));
            assertThat(parsed.getUserId()).isEqualTo(USER);
            assertThat(parsed.getInstanceId()).isEqualTo(INSTANCE);
            assertThat(parsed.getRoles()).containsExactly("USER");
        }

        @Test
        void rejectsMissingMalformedAndHistoricalFields() throws Exception {
            PassportAuthenticationException missing = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> parser.parse(SESSION, null));
            PassportAuthenticationException malformed = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> parser.parse(SESSION, "{bad"));
            ObjectNode historical = mapper.valueToTree(record());
            historical.put("rotationCounter", 1);
            PassportAuthenticationException extra = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> parser.parse(SESSION, mapper.writeValueAsString(historical)));
            assertThat(missing.getReason()).isEqualTo(PassportAuthenticationFailureReason.SESSION_MISSING);
            assertThat(malformed.getReason()).isEqualTo(PassportAuthenticationFailureReason.SESSION_MALFORMED);
            assertThat(extra.getReason()).isEqualTo(PassportAuthenticationFailureReason.SESSION_MALFORMED);
        }

        @Test
        void rejectsUnsupportedVersionAndExpiredRecord() throws Exception {
            ObjectNode unsupported = mapper.valueToTree(record());
            unsupported.put("recordVersion", 2);
            PassportAuthenticationException version = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> parser.parse(SESSION, mapper.writeValueAsString(unsupported)));
            RefreshSessionRecord expired = RefreshSessionRecord.builder()
                    .recordVersion(1).userId(USER).instanceId(INSTANCE).roles(List.of("USER"))
                    .authVersion(4L).currentRefreshTokenHash("a".repeat(64))
                    .createdAt(NOW.minus(RefreshSessionRecord.ABSOLUTE_LIFETIME))
                    .idleExpiresAt(NOW.minusSeconds(1)).absoluteExpiresAt(NOW).build();
            PassportAuthenticationException expiration = catchThrowableOfType(PassportAuthenticationException.class,
                    () -> parser.parse(SESSION, mapper.writeValueAsString(expired)));
            assertThat(version.getReason()).isEqualTo(PassportAuthenticationFailureReason.SESSION_VERSION_UNSUPPORTED);
            assertThat(expiration.getReason()).isEqualTo(PassportAuthenticationFailureReason.SESSION_EXPIRED);
        }

    }

    private RefreshSessionRecord record() {
        return RefreshSessionRecord.builder()
                .recordVersion(1)
                .userId(USER)
                .instanceId(INSTANCE)
                .roles(List.of("USER"))
                .authVersion(4L)
                .currentRefreshTokenHash("a".repeat(64))
                .createdAt(NOW)
                .idleExpiresAt(NOW.plus(RefreshSessionRecord.IDLE_LIFETIME))
                .absoluteExpiresAt(NOW.plus(RefreshSessionRecord.ABSOLUTE_LIFETIME))
                .build();
    }

}
