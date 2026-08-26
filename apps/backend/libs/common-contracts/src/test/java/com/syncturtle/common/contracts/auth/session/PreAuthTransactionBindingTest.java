package com.syncturtle.common.contracts.auth.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PreAuthTransactionBindingTest {

    @Nested
    class FromValidatedSignedCsrfToken {

        @Test
        void createsAStableNonRawBinding() {
            // arrange
            String signedToken = "test-preauth-token.test-signature";

            // act
            PreAuthTransactionBinding first = PreAuthTransactionBinding.fromValidatedSignedCsrfToken(signedToken);
            PreAuthTransactionBinding second = PreAuthTransactionBinding.fromValidatedSignedCsrfToken(signedToken);

            // assert
            assertThat(first.getValue()).hasSize(64).isEqualTo(second.getValue());
            assertThat(first.getValue()).doesNotContain(signedToken);
        }

        @Test
        void rejectsMissingValidatedToken() {
            assertThatThrownBy(() -> PreAuthTransactionBinding.fromValidatedSignedCsrfToken(" "))
                    .isInstanceOf(IllegalArgumentException.class);
        }

    }

}
