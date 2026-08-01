package com.syncturtle.services.email.messaging.kafka.consumer;

import static com.syncturtle.services.email.support.fixture.EmailFixtures.magicCodeEmailEvent;
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

import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.services.email.service.EmailInboxService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailToSendEventListener")
class EmailToSendEventListenerTest {

    @Mock
    EmailInboxService service;

    private EmailToSendEventListener listener;

    @BeforeEach
    void setup() {
        listener = new EmailToSendEventListener(service);
    }

    @Nested
    @DisplayName("onEmailToSend(EmailToSendEvent)")
    class OnEmailToSendTests {

        @Test
        @DisplayName("delegates event unchanged to public service")
        void delegatesEventUnchangedToPublicService() {
            // arrange
            EmailToSendEvent event = magicCodeEmailEvent();
            // conditions
            // act
            listener.onEmailToSend(event);
            // assert
            // verify
            verify(service).receive(event);
        }

    }

}
