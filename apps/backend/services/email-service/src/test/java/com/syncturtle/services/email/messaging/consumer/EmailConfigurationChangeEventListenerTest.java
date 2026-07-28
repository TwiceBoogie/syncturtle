package com.syncturtle.services.email.messaging.consumer;

import static org.mockito.Mockito.mock;
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

import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.services.email.service.EmailRuntimeConfigChangeService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailToSendEventListener")
class EmailConfigurationChangeEventListenerTest {

    @Mock
    EmailRuntimeConfigChangeService service;

    private EmailConfigurationChangedEventListener listener;

    @BeforeEach
    void setup() {
        listener = new EmailConfigurationChangedEventListener(service);
    }

    @Nested
    @DisplayName("onEmailConfigurationChanged(InstanceConfigurationEvent)")
    class OnEmailConfigurationChangedTests {

        @Test
        @DisplayName("delegates event unchanged to public service")
        void delegatesEventUnchangedToPublicService() {
            // arrange
            InstanceConfigurationEvent event = mock(InstanceConfigurationEvent.class);
            // conditions
            // act
            listener.onEmailConfigurationChanged(event);
            // assert
            // verify
            verify(service).receive(event);
        }

    }

}
