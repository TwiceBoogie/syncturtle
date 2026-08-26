package com.syncturtle.services.user.service.collaborator.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshSessionGraceRecord")
class RefreshSessionGraceRecordTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-08-12T12:00:05Z");

    @Nested
    @DisplayName("build")
    class BuildTests {

        @Test
        @DisplayName("constructs bounded successor reference without raw creds")
        void constructsBoundedSuccessorReferenceWithoutRawCreds() {
            // arrange
            RefreshSessionGraceRecord result = validBuilder().build();
            // conditions
            // act
            // assert
            assertThat(result.getGraceVersion()).isEqualTo(1);
            assertThat(result.getSuccessorRotationCounter()).isEqualTo(1L);
            assertThat(result.isConsumed()).isFalse();
            assertThat(RefreshSessionGraceRecord.class.getDeclaredFields())
                    .extracting(java.lang.reflect.Field::getName)
                    .doesNotContain("refreshToken", "successorRefreshToken");
        }

        @Test
        @DisplayName("rejects mismatched script deadline representation")
        void rejectsMismatchedScriptDeadlineRepresentation() {
            // arrange
            long mismatchedEpochMilli = EXPIRES_AT.plusMillis(1).toEpochMilli();
            // conditions
            // act + assert
            assertThatThrownBy(() -> validBuilder()
                    .expiresAtEpochMilli(mismatchedEpochMilli)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("expiresAtEpochMilli must represent expiresAt exactly");
            // verify
        }

        @Test
        @DisplayName("rejects same previous and successor hash")
        void rejectsSamePreviousAndSuccessorHash() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> validBuilder()
                    .successorRefreshTokenHash("a".repeat(64))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("previous and successor refresh token hashes must differ");
            // verify
        }

        @Test
        @DisplayName("rejects non positive successor counter")
        void rejectsNonPositiveSuccessorCounter() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> validBuilder().successorRotationCounter(0L).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("successorRotationCounter must be positive");
            // verify
        }

        @Test
        @DisplayName("rejects missing encrypted successor envelope")
        void rejectsMissingEncryptedSuccessorEnvelope() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> validBuilder().successorEnvelope(" ").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("successorEnvelope is required");
            // verify
        }

        @Test
        @DisplayName("rejects unsupported grace version")
        void rejectsUnsupportedGraceVersion() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> validBuilder().graceVersion(2).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("unsupported refresh session grace version");
            // verify
        }

    }

    private static RefreshSessionGraceRecord.RefreshSessionGraceRecordBuilder validBuilder() {
        return RefreshSessionGraceRecord.builder()
                .graceVersion(RefreshSessionGraceRecord.CURRENT_GRACE_VERSION)
                .previousRefreshTokenHash("a".repeat(64))
                .successorRefreshTokenHash("b".repeat(64))
                .successorRotationCounter(1L)
                .successorEnvelope("encrypted-successor-envelope")
                .clientBindingHash("c".repeat(64))
                .expiresAt(EXPIRES_AT)
                .expiresAtEpochMilli(EXPIRES_AT.toEpochMilli())
                .consumed(false);
    }

}
