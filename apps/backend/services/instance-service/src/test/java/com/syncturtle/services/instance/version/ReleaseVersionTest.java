package com.syncturtle.services.instance.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.instance.type.VersionStatus;

@DisplayName("Release version comparison")
class ReleaseVersionTest {

    @Nested
    class Status {

        @Test
        void comparesNumericComponentsInsteadOfText() {
            assertThat(ReleaseVersion.status("1.2.9", "v1.2.10"))
                    .isEqualTo(VersionStatus.UPDATE_AVAILABLE);
        }

        @Test
        void treatsSnapshotAsPrerelease() {
            assertThat(ReleaseVersion.status("1.2.3-SNAPSHOT", "1.2.3"))
                    .isEqualTo(VersionStatus.UPDATE_AVAILABLE);
        }

        @Test
        void ignoresBuildMetadataForPrecedence() {
            assertThat(ReleaseVersion.status("1.2.3+local", "v1.2.3+release"))
                    .isEqualTo(VersionStatus.CURRENT);
        }

        @Test
        void recognizesAheadOfRelease() {
            assertThat(ReleaseVersion.status("2.0.0", "1.9.9"))
                    .isEqualTo(VersionStatus.AHEAD_OF_RELEASE);
        }

        @Test
        void leavesDevelopmentAndMissingReleaseUnknown() {
            assertThat(ReleaseVersion.status("development", "1.0.0"))
                    .isEqualTo(VersionStatus.UNKNOWN);
            assertThat(ReleaseVersion.status("1.0.0", null))
                    .isEqualTo(VersionStatus.UNKNOWN);
        }

        @Test
        void rejectsMalformedTags() {
            assertThat(ReleaseVersion.status("1.0.0", "release-1.1"))
                    .isEqualTo(VersionStatus.UNKNOWN);
            assertThat(ReleaseVersion.parse("1.2.3-01")).isEmpty();
        }
    }

}
