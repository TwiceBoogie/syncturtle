package com.syncturtle.services.workspace.messaging.kafka.consumer;

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
import com.syncturtle.services.workspace.service.collaborator.runtime.WorkspaceConfigResolver;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("WorkspaceFlagConfigurationChangeEventListener")
class WorkspaceFlagConfigurationChangeEventListenerTest {

    @Mock
    WorkspaceConfigResolver resolver;

    private WorkspaceFlagConfigurationChangeEventListener listener;

    @BeforeEach
    void setup() {
        listener = new WorkspaceFlagConfigurationChangeEventListener(resolver);
    }

    @Nested
    @DisplayName("onInstanceConfigurationEvent(InstanceConfigurationEvent)")
    class OnInstanceConfigurationEventTests {

        @Test
        @DisplayName("evicts shared redis for relevant and whole scope events")
        void evictsSharedRedisForRelevantAndWholeScopeEvents() {
            // arrange
            InstanceConfigurationEvent event = event(InstanceConfigurationScope.WORKSPACE,
                    Set.of("DISABLE_WORKSPACE_CREATION"));
            // conditions
            // act
            listener.onInstanceConfigurationChanged(event);
            // assert
            // verify
            verify(resolver).evict();
        }

        @Test
        @DisplayName("ignores irrelevant scope before version processing")
        void ignoresIrrelevantScopeBeforeVersionProcessing() {
            // arrange
            InstanceConfigurationEvent event = event(InstanceConfigurationScope.AUTH, Set.of("ENABLE_SIGNUP"));
            // conditions
            // act
            listener.onInstanceConfigurationChanged(event);
            // assert
            // verify
            verifyNoInteractions(resolver);
        }

    }

    private static InstanceConfigurationEvent event(InstanceConfigurationScope scope, Set<String> keys) {
        return InstanceConfigurationEvent.builder()
                .eventId("event-1")
                .occurredAt(Instant.parse("2026-07-29T12:00:00Z"))
                .instanceId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
                .scope(scope)
                .configurationVersion(7L)
                .changedKeys(keys)
                .build();
    }

}
