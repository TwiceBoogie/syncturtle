package com.syncturtle.services.email.model.param;

import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.ERROR_MESSAGE;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.MAX_ATTEMPTS;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.RETRY_DELAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmailEventInboxRetryFailureParam")
class EmailEventInboxRetryFailureParamTest {

    @Test
    @DisplayName("accepts positive retry policy and error summary")
    void acceptsPositiveRetryPolicyAndErrorSummary() {
        EmailEventInboxRetryFailureParam param = new EmailEventInboxRetryFailureParam(MAX_ATTEMPTS, RETRY_DELAY,
                ERROR_MESSAGE);

        assertThat(param.getMaxAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(param.getRetryDelay()).isEqualTo(RETRY_DELAY);
        assertThat(param.getErrorMessage()).isEqualTo(ERROR_MESSAGE);
    }

    @Test
    @DisplayName("rejects zero retry delay")
    void rejectsZeroRetryDelay() {
        assertThatThrownBy(() -> new EmailEventInboxRetryFailureParam(MAX_ATTEMPTS, Duration.ZERO, ERROR_MESSAGE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("retryDelay must be positive");
    }

    @Test
    @DisplayName("rejects error summary longer than database column")
    void rejectsErrorSummaryLongerThanDatabaseColumn() {
        assertThatThrownBy(() -> new EmailEventInboxRetryFailureParam(MAX_ATTEMPTS, RETRY_DELAY, "x".repeat(4_001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("errorMessage must be 4000 characters or fewer");
    }
}
