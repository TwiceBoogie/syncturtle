package com.syncturtle.services.email.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

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
            InstanceConfigurationEvent event = event(InstanceConfigurationScope.AUTH, 9L, Set.of("ENABLE_SIGNUP"));
            // conditions
            // act
            service.receive(event);
            // assert
            // verify
            verifyNoInteractions(runtimeConfigResolver);
        }

        @Test
        @DisplayName("refreshes email for relavent and whole scope notifications")
        void refreshesEmailNotifications() {
            // arrange
            InstanceConfigurationEvent event = event(InstanceConfigurationScope.EMAIL, 9L, Set.of("FUTURE_EMAIL_KEY"));
            // conditions
            // act
            service.receive(event);
            // assert
            // verify
            verify(runtimeConfigResolver).refreshIfOlderThan(9L);
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

    private static InstanceConfigurationEvent event(InstanceConfigurationScope scope, long version,
            Set<String> changedKeys) {
        return InstanceConfigurationEvent.builder()
                .eventId("event-" + version)
                .occurredAt(Instant.parse("2026-07-29T12:00:00Z"))
                .instanceId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .scope(scope)
                .configurationVersion(version)
                .changedKeys(changedKeys)
                .build();
    }

}
