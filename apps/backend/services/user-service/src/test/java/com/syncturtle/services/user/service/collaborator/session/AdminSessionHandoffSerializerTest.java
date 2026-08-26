package com.syncturtle.services.user.service.collaborator.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.user.exception.AdminSessionHandoffException;
import com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures;
import com.syncturtle.services.user.type.AdminSessionHandoffState;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@DisplayName("AdminSessionHandoffSerializer")
class AdminSessionHandoffSerializerTest {

    private JsonMapper jsonMapper;
    private AdminSessionHandoffSerializer serializer;

    @BeforeEach
    void setup() {
        jsonMapper = JsonMapper.builder().findAndAddModules().build();
        serializer = new AdminSessionHandoffSerializer(jsonMapper);
    }

    @Nested
    @DisplayName("encode/decode")
    class EncodeDecodeTests {

        @Test
        @DisplayName("round trips pending state with an explicit null claim id")
        void roundTripsPendingStateWithExplicitNullClaimId() throws Exception {
            // arrange
            AdminSessionHandoffRecord pending = AdminSessionHandoffTestFixtures.pendingRecord();
            // act
            String json = serializer.encode(pending);
            AdminSessionHandoffRecord decoded = serializer.decode(json);
            // assert
            assertThat(jsonMapper.readTree(json).has("claimId")).isTrue();
            assertThat(jsonMapper.readTree(json).get("claimId").isNull()).isTrue();
            assertThat(decoded.getState()).isEqualTo(AdminSessionHandoffState.PENDING);
            assertThat(decoded.getClaimId()).isNull();
        }

        @Test
        @DisplayName("rejects a missing claim id field")
        void rejectsMissingClaimIdField() throws Exception {
            // arrange
            ObjectNode root = (ObjectNode) jsonMapper.readTree(
                    serializer.encode(AdminSessionHandoffTestFixtures.pendingRecord()));
            root.remove("claimId");
            // act
            AdminSessionHandoffException failure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> serializer.decode(root.toString()));
            // assert
            assertThat(failure.getReason()).isEqualTo(AdminSessionHandoffException.Reason.MALFORMED_RECORD);
        }

        @Test
        @DisplayName("rejects unsupported and malformed record versions distinctly")
        void rejectsInvalidRecordVersions() throws Exception {
            // arrange
            ObjectNode unsupported = encodedPendingObject();
            unsupported.put("recordVersion", 2);
            ObjectNode malformed = encodedPendingObject();
            malformed.put("recordVersion", "1");
            // act
            AdminSessionHandoffException unsupportedFailure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> serializer.decode(unsupported.toString()));
            AdminSessionHandoffException malformedFailure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> serializer.decode(malformed.toString()));
            // assert
            assertThat(unsupportedFailure.getReason())
                    .isEqualTo(AdminSessionHandoffException.Reason.UNSUPPORTED_RECORD_VERSION);
            assertThat(malformedFailure.getReason())
                    .isEqualTo(AdminSessionHandoffException.Reason.MALFORMED_RECORD);
        }

        @Test
        @DisplayName("rejects unsupported fields and noncanonical claim ids")
        void rejectsUnsupportedFieldsAndNoncanonicalClaimIds() throws Exception {
            // arrange
            ObjectNode unsupported = encodedPendingObject();
            unsupported.put("accessToken", "forbidden");
            ObjectNode noncanonicalClaim = (ObjectNode) jsonMapper.readTree(
                    serializer.encode(AdminSessionHandoffTestFixtures.claimedRecord()));
            noncanonicalClaim.put("claimId", "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA");
            // act
            AdminSessionHandoffException unsupportedFailure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> serializer.decode(unsupported.toString()));
            AdminSessionHandoffException claimFailure = catchThrowableOfType(
                    AdminSessionHandoffException.class,
                    () -> serializer.decode(noncanonicalClaim.toString()));
            // assert
            assertThat(unsupportedFailure.getReason())
                    .isEqualTo(AdminSessionHandoffException.Reason.MALFORMED_RECORD);
            assertThat(claimFailure.getReason())
                    .isEqualTo(AdminSessionHandoffException.Reason.MALFORMED_RECORD);
        }

        private ObjectNode encodedPendingObject() throws Exception {
            return (ObjectNode) jsonMapper.readTree(
                    serializer.encode(AdminSessionHandoffTestFixtures.pendingRecord()));
        }

    }

}
