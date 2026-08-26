package com.syncturtle.common.contracts.auth.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RefreshSessionFamilyRecordTest {

    private static final String USER_ID = "c12c7988-b9fe-4299-a72f-e430d004d37b";
    private static final String INSTANCE_ID = "91ff9854-e8ac-4bf5-91d8-3117dcd8d05c";
    private static final String TOKEN_HASH = "a".repeat(64);
    private static final String CLIENT_BINDING_HASH = "b".repeat(64);
    private static final Instant CREATED_AT = Instant.parse("2026-08-10T12:00:00Z");
    private static final Instant LAST_USED_AT = Instant.parse("2026-08-12T12:00:00Z");
    private static final Instant IDLE_EXPIRES_AT = Instant.parse("2026-08-19T12:00:00Z");
    private static final Instant ABSOLUTE_EXPIRES_AT = Instant.parse("2026-09-09T12:00:00Z");

    @Nested
    class Build {

        @Test
        void constructsCanonicalMemberFamily() {
            RefreshSessionFamilyRecord result = memberRecordBuilder().build();

            assertThat(result.getRecordVersion()).isEqualTo(2);
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getRoles()).containsExactly("USER");
            assertThat(result.getAdminSessionVersion()).isNull();
            assertThat(result.getRotationCounter()).isZero();
            assertThat(result.nextRotationCounter()).isEqualTo(1L);
        }

        @Test
        void constructsCanonicalAdministratorFamily() {
            RefreshSessionFamilyRecord result = memberRecordBuilder()
                    .roles(List.of("INSTANCE_ADMIN"))
                    .adminSessionVersion(7L)
                    .rotationCounter(3L)
                    .build();

            assertThat(result.getRoles()).containsExactly("INSTANCE_ADMIN");
            assertThat(result.getAdminSessionVersion()).isEqualTo(7L);
            assertThat(result.nextRotationCounter()).isEqualTo(4L);
        }

        @Test
        void normalizesOptionalDeviceLabelWithoutPersistingRawClientMetadata() {
            RefreshSessionFamilyRecord result = memberRecordBuilder()
                    .deviceLabel("  Firefox on macOS  ")
                    .build();

            assertThat(result.getDeviceLabel()).isEqualTo("Firefox on macOS");
            assertThat(RefreshSessionFamilyRecord.class.getDeclaredFields())
                    .extracting(java.lang.reflect.Field::getName)
                    .doesNotContain(
                            "sessionId",
                            "familyId",
                            "email",
                            "active",
                            "refreshToken",
                            "ipAddress",
                            "userAgent");
        }

        @Test
        void rejectsUnsupportedRecordVersion() {
            assertThatThrownBy(() -> memberRecordBuilder().recordVersion(1).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("unsupported refresh session family record version");
        }

        @Test
        void rejectsNegativeAuthenticationVersion() {
            assertThatThrownBy(() -> memberRecordBuilder().authVersion(-1L).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("authVersion must not be negative");
        }

        @Test
        void rejectsMissingAuthenticationVersion() {
            assertThatThrownBy(() -> memberRecordBuilder().authVersion(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("authVersion is required");
        }

        @Test
        void rejectsNegativeRotationCounter() {
            assertThatThrownBy(() -> memberRecordBuilder().rotationCounter(-1L).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("rotationCounter must be between 0 and Long.MAX_VALUE - 1");
        }

        @Test
        void rejectsUnadvanceableRotationCounter() {
            assertThatThrownBy(() -> memberRecordBuilder().rotationCounter(Long.MAX_VALUE).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("rotationCounter must be between 0 and Long.MAX_VALUE - 1");
        }

        @Test
        void rejectsMissingRotationCounter() {
            assertThatThrownBy(() -> memberRecordBuilder().rotationCounter(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("rotationCounter is required");
        }

        @Test
        void rejectsUnsortedRoles() {
            assertThatThrownBy(() -> memberRecordBuilder()
                    .roles(List.of("USER", "INSTANCE_ADMIN"))
                    .adminSessionVersion(7L)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("roles must be unique and sorted");
        }

        @Test
        void rejectsDuplicateRoles() {
            assertThatThrownBy(() -> memberRecordBuilder().roles(List.of("USER", "USER")).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("roles must be unique and sorted");
        }

        @Test
        void rejectsAdminRoleWithoutAdminVersion() {
            assertThatThrownBy(() -> memberRecordBuilder()
                    .roles(List.of("INSTANCE_ADMIN", "USER"))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("INSTANCE_ADMIN role and adminSessionVersion");
        }

        @Test
        void rejectsAdminVersionWithoutAdminRole() {
            assertThatThrownBy(() -> memberRecordBuilder().adminSessionVersion(7L).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("INSTANCE_ADMIN role and adminSessionVersion");
        }

        @Test
        void rejectsNegativeAdminVersion() {
            assertThatThrownBy(() -> memberRecordBuilder()
                    .roles(List.of("INSTANCE_ADMIN"))
                    .adminSessionVersion(-1L)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("adminSessionVersion must not be negative");
        }

        @Test
        void rejectsLastUseBeforeCreation() {
            assertThatThrownBy(() -> memberRecordBuilder()
                    .lastUsedAt(CREATED_AT.minusSeconds(1))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("lastUsedAt must not be before createdAt");
        }

        @Test
        void rejectsAbsoluteDeadlineAtCreation() {
            assertThatThrownBy(() -> memberRecordBuilder()
                    .absoluteExpiresAt(CREATED_AT)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("absoluteExpiresAt must be after createdAt");
        }

        @Test
        void rejectsIdleDeadlineAtLastUse() {
            assertThatThrownBy(() -> memberRecordBuilder()
                    .idleExpiresAt(LAST_USED_AT)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("idleExpiresAt must be after lastUsedAt");
        }

        @Test
        void rejectsIdleDeadlineAfterAbsoluteDeadline() {
            assertThatThrownBy(() -> memberRecordBuilder()
                    .idleExpiresAt(ABSOLUTE_EXPIRES_AT.plusSeconds(1))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("idleExpiresAt must not be after absoluteExpiresAt");
        }

        @Test
        void rejectsAbsoluteDeadlineThatDoesNotPreserveThirtyDayBoundary() {
            assertThatThrownBy(() -> memberRecordBuilder()
                    .absoluteExpiresAt(ABSOLUTE_EXPIRES_AT.minusSeconds(1))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("absoluteExpiresAt must be exactly 30 days after createdAt");
        }

        @Test
        void rejectsIdleDeadlineThatDoesNotPreserveSevenDayBoundary() {
            assertThatThrownBy(() -> memberRecordBuilder()
                    .idleExpiresAt(IDLE_EXPIRES_AT.minusSeconds(1))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("idleExpiresAt must equal the earlier of 7 days after lastUsedAt");
        }

        @Test
        void rejectsNonSha256CurrentTokenHash() {
            assertThatThrownBy(() -> memberRecordBuilder().currentRefreshTokenHash("not-a-hash").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("currentRefreshTokenHash must be a lowercase SHA-256 hex value");
        }

        @Test
        void rejectsOversizedDeviceLabel() {
            assertThatThrownBy(() -> memberRecordBuilder().deviceLabel("x".repeat(121)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("deviceLabel must be 120 characters or fewer");
        }
    }

    private static RefreshSessionFamilyRecord.RefreshSessionFamilyRecordBuilder memberRecordBuilder() {
        return RefreshSessionFamilyRecord.builder()
                .recordVersion(RefreshSessionFamilyRecord.CURRENT_RECORD_VERSION)
                .userId(USER_ID)
                .instanceId(INSTANCE_ID)
                .roles(List.of("USER"))
                .authVersion(4L)
                .currentRefreshTokenHash(TOKEN_HASH)
                .rotationCounter(0L)
                .createdAt(CREATED_AT)
                .lastUsedAt(LAST_USED_AT)
                .idleExpiresAt(IDLE_EXPIRES_AT)
                .absoluteExpiresAt(ABSOLUTE_EXPIRES_AT)
                .deviceLabel("Firefox on macOS")
                .clientBindingHash(CLIENT_BINDING_HASH);
    }
}
