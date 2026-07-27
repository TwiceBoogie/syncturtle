package com.syncturtle.services.email.service.runtime;

import static com.syncturtle.services.email.support.fixture.EmailRuntimeConfigFixtures.SMTP_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EmailRuntimeConfigSnapshot")
class EmailRuntimeConfigSnapshotTest {

    @Nested
    @DisplayName("isComplete()")
    class IsCompleteTests {

        @Test
        @DisplayName("returns true for enabled configuration with host, port, and from")
        void returnsTrueForEnabledConfigurationWithHostPortAndFrom() {
            // arrange
            EmailRuntimeConfigSnapshot snapshot = EmailRuntimeConfigSnapshot.builder()
                    .enabled(true)
                    .host(" smtp.example.com ")
                    .port(2525)
                    .from(" no-reply@syncturtle.com ")
                    .password(SMTP_PASSWORD)
                    .version(1)
                    .build();
            // conditions
            // act
            // assert
            assertThat(snapshot.isComplete()).isTrue();
            assertThat(snapshot.getHost()).isEqualTo("smtp.example.com");
            assertThat(snapshot.getFrom()).isEqualTo("no-reply@syncturtle.com");
            assertThat(snapshot.getPassword()).isEqualTo(SMTP_PASSWORD);
            // verify
        }

        @Test
        @DisplayName("returns false when SMTP is disabled")
        void returnsFalseWhenSmtpIsDisabled() {
            EmailRuntimeConfigSnapshot snapshot = EmailRuntimeConfigSnapshot.builder()
                    .enabled(false)
                    .host("smtp.example.com")
                    .port(2525)
                    .from("no-reply@syncturtle.com")
                    .version(1)
                    .build();

            assertThat(snapshot.isComplete()).isFalse();
        }

    }

    @Nested
    @DisplayName("requiresAuthentication()")
    class RequiresAuthenticationTests {

        @Test
        @DisplayName("returns true only when normalized username exists")
        void returnsTrueOnlyWhenNormalizedUsernameExists() {
            EmailRuntimeConfigSnapshot authenticated = EmailRuntimeConfigSnapshot.builder()
                    .enabled(true).host("smtp.example.com").port(2525).from("no-reply@syncturtle.com")
                    .username(" user ").version(1).build();
            EmailRuntimeConfigSnapshot unauthenticated = EmailRuntimeConfigSnapshot.builder()
                    .enabled(true).host("smtp.example.com").port(2525).from("no-reply@syncturtle.com")
                    .username(" ").version(1).build();

            assertThat(authenticated.requiresAuthentication()).isTrue();
            assertThat(unauthenticated.requiresAuthentication()).isFalse();
        }
    }

    @Test
    @DisplayName("rejects port outside valid network range")
    void rejectsPortOutsideValidNetworkRange() {
        assertThatThrownBy(() -> EmailRuntimeConfigSnapshot.builder().enabled(true).port(65_536).version(1).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("port must be between 1 and 65535");
    }

}
