package com.syncturtle.services.user.service.collaborator.session;

import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.CLAIM_ID;
import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.EXPIRES_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures;
import com.syncturtle.services.user.type.AdminSessionHandoffState;

@DisplayName("AdminSessionHandoffRecord")
class AdminSessionHandoffRecordTest {

    @Nested
    @DisplayName("construction")
    class ConstructionTests {

        @Test
        @DisplayName("constructs pending and claimed state with one machine expiry")
        void constructsPendingAndClaimedState() {
            // arrange
            // act
            AdminSessionHandoffRecord pending = AdminSessionHandoffTestFixtures.pendingRecord();
            AdminSessionHandoffRecord claimed = AdminSessionHandoffTestFixtures.claimedRecord();
            // assert
            assertThat(pending.getState()).isEqualTo(AdminSessionHandoffState.PENDING);
            assertThat(pending.getClaimId()).isNull();
            assertThat(claimed.getState()).isEqualTo(AdminSessionHandoffState.CLAIMED);
            assertThat(claimed.getClaimId()).isEqualTo(CLAIM_ID);
            assertThat(AdminSessionHandoffRecord.class.isRecord()).isFalse();
            assertThat(AdminSessionHandoffRecord.class.getRecordComponents())
                    .isNullOrEmpty();
            assertThat(AdminSessionHandoffRecord.class.getDeclaredFields())
                    .extracting(java.lang.reflect.Field::getName)
                    .doesNotContain("completionCode", "accessToken", "refreshToken", "clientIp", "userAgent");
        }

        @Test
        @DisplayName("enforces the state and claim id biconditional")
        void enforcesTheStateAndClaimIdBiconditional() {
            // arrange
            // act + assert
            assertThatThrownBy(() -> AdminSessionHandoffTestFixtures.baseRecordBuilder()
                    .state(AdminSessionHandoffState.PENDING)
                    .claimId(CLAIM_ID)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> AdminSessionHandoffTestFixtures.baseRecordBuilder()
                    .state(AdminSessionHandoffState.CLAIMED)
                    .claimId(null)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("requires a canonical claim UUID")
        void requiresACanonicalClaimUuid() {
            // arrange
            String uppercase = "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA";
            // act + assert
            assertThatThrownBy(() -> AdminSessionHandoffTestFixtures.baseRecordBuilder()
                    .state(AdminSessionHandoffState.CLAIMED)
                    .claimId(uppercase)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("canonical UUID");
        }

        @Test
        @DisplayName("rejects nonpositive and overlong lifetimes")
        void rejectsInvalidLifetimes() {
            // arrange
            long issuedAt = AdminSessionHandoffTestFixtures.ISSUED_AT.toEpochMilli();
            // act + assert
            assertThatThrownBy(() -> AdminSessionHandoffTestFixtures.baseRecordBuilder()
                    .expiresAtEpochMilli(issuedAt)
                    .state(AdminSessionHandoffState.PENDING)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> AdminSessionHandoffTestFixtures.baseRecordBuilder()
                    .expiresAtEpochMilli(EXPIRES_AT.plusMillis(1).toEpochMilli())
                    .state(AdminSessionHandoffState.PENDING)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

}
