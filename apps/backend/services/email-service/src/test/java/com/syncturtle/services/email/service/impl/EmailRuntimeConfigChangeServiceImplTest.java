package com.syncturtle.services.email.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.services.email.service.EmailRuntimeConfigChangeService;
import com.syncturtle.services.email.service.collaborator.EmailRuntimeConfigResolver;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("EmailRuntimeConfigChangeService")
class EmailRuntimeConfigChangeServiceImplTest {

    @Mock
    EmailRuntimeConfigResolver runtimeConfigResolver;
    @Mock
    InstanceConfigurationEvent event;

    private EmailRuntimeConfigChangeService service;

    @BeforeEach
    void setup() {
        service = new EmailRuntimeConfigChangeServiceImpl(runtimeConfigResolver);
    }

    @Nested
    @DisplayName("receive(InstanceConfigurationEvent)")
    class ReceiveTests {

        @Test
        @DisplayName("ignores configuration event for non-email scope")
        void ignoresConfigurationEventForNonEmailScope() {
            // arrange
            // conditions
            when(event.getScope()).thenReturn(InstanceConfigurationScope.AUTH);
            when(event.getEventId()).thenReturn("event-1");
            // act
            service.receive(event);
            // assert
            // verify
            verifyNoInteractions(runtimeConfigResolver);
        }

        @Test
        @DisplayName("refreshes email runtime configuration for event global version")
        void refreshesEmailRuntimeConfigurationForEventGlobalVersion() {
            // arrange
            // conditions
            when(event.getScope()).thenReturn(InstanceConfigurationScope.EMAIL);
            when(event.getGlobalVersion()).thenReturn(12L);
            when(event.getScopeVersion()).thenReturn(4L);
            when(event.getEventId()).thenReturn("event-1");
            // act
            service.receive(event);
            // assert
            // verify
            verify(runtimeConfigResolver).refreshIfOlderThan(12L);
        }

        @Test
        @DisplayName("rejects null event")
        void rejectsNullEvent() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.receive(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("instance configuration event is required");
            // verify
            verifyNoInteractions(runtimeConfigResolver);
        }

    }

}
