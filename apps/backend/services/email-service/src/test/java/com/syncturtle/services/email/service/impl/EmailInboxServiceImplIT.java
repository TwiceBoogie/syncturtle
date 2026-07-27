package com.syncturtle.services.email.service.impl;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.EVENT_ID;
import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeEmailEvent;
import static com.syncturtle.services.email.support.fixture.EmailInboxFixtures.inboxProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.services.email.configuration.property.EmailInboxProperties;
import com.syncturtle.services.email.model.EmailEventInbox;
import com.syncturtle.services.email.repository.EmailEventInboxRepository;
import com.syncturtle.services.email.service.EmailInboxService;
import com.syncturtle.services.email.service.inbox.EmailInboxProcessor;
import com.syncturtle.services.email.service.inbox.EmailInboxStore;
import com.syncturtle.services.email.support.clock.TestClocks;
import com.syncturtle.services.email.type.EmailEventInboxStatus;
import com.syncturtle.testing.annotations.JpaIntegrationTest;
import com.syncturtle.testing.annotations.UsePostgresDb;

import tools.jackson.databind.json.JsonMapper;

@JpaIntegrationTest
@UsePostgresDb("email_service_inbox_service_it")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        EmailInboxStore.class,
        EmailInboxServiceImpl.class,
        EmailInboxServiceImplIT.InboxTestConfiguration.class
})
@DisplayName("EmailInboxService")
class EmailInboxServiceImplIT {

    @Autowired
    EmailInboxService service;
    @Autowired
    EmailInboxStore store;
    @Autowired
    EmailEventInboxRepository repository;

    @MockitoBean
    EmailInboxProcessor inboxProcessor;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAllInBatch();
    }

    @Nested
    @DisplayName("receive(EmailToSendEvent)")
    class ReceiveTests {

        @Test
        @DisplayName("resolves duplicate event after fresh insert transaction fails")
        void resolvesDuplicateEventAfterFreshInsertTransactionFails() {
            // arrange
            EmailToSendEvent event = magicCodeEmailEvent();
            ArgumentCaptor<EmailEventInbox> rowCaptor = ArgumentCaptor.forClass(EmailEventInbox.class);
            // conditions
            // act: first delivery inserts the row
            service.receive(event);
            // assert
            // verify + capture
            verify(inboxProcessor).process(rowCaptor.capture());
            EmailEventInbox inserted = rowCaptor.getValue();

            assertThat(inserted.getEventId()).isEqualTo(EVENT_ID);
            assertThat(repository.count()).isEqualTo(1);

            clearInvocations(inboxProcessor);
            // act: second delivery violates the unique event constraint
            service.receive(event);
            // assert: exisitng PROCESSING row is not processed again
            verifyNoInteractions(inboxProcessor);

            EmailEventInbox persisted = repository.findByEventId(EVENT_ID).orElseThrow();

            assertThat(repository.count()).isEqualTo(1);
            assertThat(persisted.getStatus()).isEqualTo(EmailEventInboxStatus.PROCESSING);
            assertThat(persisted.getAttemptCount()).isEqualTo(1);
        }

    }

    @TestConfiguration(proxyBeanMethods = false)
    static class InboxTestConfiguration {

        @Bean
        Clock clock() {
            return TestClocks.fixedUtc();
        }

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        EmailInboxProperties emailInboxProperties() {
            return inboxProperties();
        }

    }

}
