package com.syncturtle.services.email.service.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.type.EmailInboxAcquireDecision;

@DisplayName("EmailInboxAcquireResult")
class EmailInboxAcquireResultTest {

    @Test
    @DisplayName("factory methods preserve decision and row")
    void factoryMethodsPreserveDecisionAndRow() {
        // arrange
        EmailEventInbox row = mock(EmailEventInbox.class);
        // conditions
        // act + assert
        assertThat(EmailInboxAcquireResult.acquired(row).getDecision())
                .isEqualTo(EmailInboxAcquireDecision.ACQUIRED);
        assertThat(EmailInboxAcquireResult.retryScheduled(row).getDecision())
                .isEqualTo(EmailInboxAcquireDecision.RETRY_SCHEDULED);
        assertThat(EmailInboxAcquireResult.permanentFailure(row).getRow()).isSameAs(row);
    }

    @Test
    @DisplayName("rejects null row")
    void rejectsNullRow() {
        assertThatThrownBy(() -> EmailInboxAcquireResult.acquired(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email inbox row is required");
    }

}
