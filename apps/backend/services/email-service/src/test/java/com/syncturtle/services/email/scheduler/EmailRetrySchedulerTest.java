package com.syncturtle.services.email.scheduler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.services.email.service.EmailInboxService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailRetryScheduler")
public class EmailRetrySchedulerTest {

    @Mock
    EmailInboxService service;

    private EmailRetryScheduler scheduler;

    @BeforeEach
    void setup() {
        scheduler = new EmailRetryScheduler(service);
    }

    @Nested
    @DisplayName("retryDueEmails()")
    class RetryDueEmailsTests {

        @Test
        @DisplayName("delegates retry polling to public service")
        void delegatesRetryPollingToPublicService() {
            // arrange
            // conditions
            // act
            scheduler.retryDueEmails();
            // assert
            // verify
            verify(service).retryDueEmails();
        }

    }

}
