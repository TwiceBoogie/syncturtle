package com.syncturtle.services.email.integration.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.syncturtle.services.email.enums.EmailEventInboxStatus;
import com.syncturtle.services.email.models.EmailEventInbox;
import com.syncturtle.services.email.repositories.EmailEventInboxRepository;
import com.syncturtle.testing.annotations.JpaIntegrationTest;
import com.syncturtle.testing.annotations.UsePostgresDb;

@JpaIntegrationTest
@UsePostgresDb("email_service_it")
class EmailEventInboxRepositoryIT {

    @Autowired
    EmailEventInboxRepository repository;

    @Test
    void lockDueRetryIds_whenRowsAreDue_returnsOnlyDueRows() {
        EmailEventInbox due = new EmailEventInbox();
        due.setEventId("evt-due");
        due.setEventType("EmailToSendEvent");
        due.setTemplateType("MAGIC_LINK");
        due.setSubject("subject");
        due.setRecipientToJson("[\"user@example.com\"]");
        due.setTemplateModelJson("{\"magicLink\":\"https://example.com\"}");
        due.setStatus(EmailEventInboxStatus.FAILED_RETRYABLE);
        due.setAttemptCount(1);
        due.setNextAttemptAt(Instant.now().minusSeconds(30));

        EmailEventInbox notDue = new EmailEventInbox();
        notDue.setEventId("evt-not-due");
        notDue.setEventType("EmailToSendEvent");
        notDue.setTemplateType("MAGIC_LINK");
        notDue.setSubject("subject");
        notDue.setRecipientToJson("[\"user@example.com\"]");
        notDue.setTemplateModelJson("[\"magicLink\":\"https://example.com\"}");
        notDue.setStatus(EmailEventInboxStatus.FAILED_RETRYABLE);
        notDue.setAttemptCount(1);
        notDue.setNextAttemptAt(Instant.now().plusSeconds(300));

        repository.saveAllAndFlush(List.of(due, notDue));

        List<UUID> ids = repository.lockDueRetryIds(Instant.now(), 25);

        assertThat(ids).contains(due.getId());
        assertThat(ids).doesNotContain(notDue.getId());
    }

}
