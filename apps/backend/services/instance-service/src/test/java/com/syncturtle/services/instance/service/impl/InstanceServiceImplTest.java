package com.syncturtle.services.instance.service.impl;

import static com.syncturtle.services.instance.support.assertion.PublishedInstanceEventAssert.assertThatPublishedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.web.property.PublicUrlProperties;
import com.syncturtle.services.instance.dto.request.InstanceUpdateRequest;
import com.syncturtle.services.instance.dto.response.InstanceResponse;
import com.syncturtle.services.instance.dto.response.InstanceSetupConfigResponse;
import com.syncturtle.services.instance.dto.response.InstanceSetupResponse;
import com.syncturtle.services.instance.mapper.InstanceApiMapper;
import com.syncturtle.services.instance.mapper.InstanceConfigurationApiMapper;
import com.syncturtle.services.instance.messaging.kafka.factory.InstanceEventFactory;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.UserRepository;
import com.syncturtle.services.instance.repository.WorkspaceRepository;
import com.syncturtle.services.instance.service.InstanceService;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.collaborator.outbox.InstanceOutboxWriter;
import com.syncturtle.services.instance.service.param.RequestedKeyParam;
import com.syncturtle.services.instance.support.clock.TestClocks;
import com.syncturtle.services.instance.support.fixture.InstanceEventFixtures;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;
import com.syncturtle.services.instance.support.fixture.PublicUrlFixtures;
import com.syncturtle.services.instance.support.fixture.ResponseFixtures;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("InstanceService")
class InstanceServiceImplTest {

    @Mock
    InstanceRepository instanceRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    WorkspaceRepository workspaceRepository;
    @Mock
    InstanceOutboxWriter outboxWriter;
    @Mock
    InstanceEventFactory eventFactory;
    @Mock
    InstanceConfigurationResolver resolver;
    @Mock
    InstanceApiMapper instanceApiMapper;
    @Mock
    InstanceConfigurationApiMapper configurationApiMapper;
    @Captor
    private ArgumentCaptor<InstanceEvent> eventCaptor;
    @Captor
    private ArgumentCaptor<List<RequestedKeyParam>> requestedKeysCaptor;

    private PublicUrlProperties publicUrls;
    private InstanceService service;

    @BeforeEach
    void setup() {
        publicUrls = PublicUrlFixtures.publicUrls();
        service = new InstanceServiceImpl(
                instanceRepository,
                userRepository,
                workspaceRepository,
                outboxWriter,
                eventFactory,
                resolver,
                instanceApiMapper,
                configurationApiMapper,
                publicUrls);
    }

    @Nested
    @DisplayName("getPublicInstance()")
    class getPublicInstanceTests {

        @Test
        @DisplayName("returns inactive response when no configured instance exists")
        void returnsInactiveResponseWhenNoConfiguredInstanceExists() {
            // arrange
            InstanceSetupResponse expected = ResponseFixtures.inactiveSetupResponse();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.empty());
            when(instanceApiMapper.toInactiveInstanceResponse()).thenReturn(expected);
            // act
            InstanceSetupResponse actual = service.getPublicInstance();
            // assert
            assertThat(actual).isSameAs(expected);
            // verify
            verify(instanceRepository).findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class);
            verify(instanceApiMapper).toInactiveInstanceResponse();
            verifyNoInteractions(resolver, userRepository, configurationApiMapper, outboxWriter);
            verifyNoMoreInteractions(instanceRepository, instanceApiMapper);
        }

        @Test
        @DisplayName("resolves config, counts active users, maps the setup response")
        void resolvesConfigCountsUsersAndMapsResponse() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            InstanceSetupConfigResponse configResponse = ResponseFixtures.setupConfigResponse();
            InstanceResponse instanceResponse = ResponseFixtures.instanceResponse("Syncturtle", 3L);
            InstanceSetupResponse expected = InstanceSetupResponse.builder()
                    .config(configResponse)
                    .instance(instanceResponse)
                    .build();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(resolver.resolveRequested(anyList())).thenAnswer(invocation -> {
                List<RequestedKeyParam> requested = invocation.getArgument(0);
                Map<InstanceConfigurationKey, String> result = new EnumMap<>(
                        InstanceConfigurationKey.class);

                for (RequestedKeyParam requestedKey : requested) {
                    InstanceConfigurationKey key = requestedKey.getKey();
                    if (key == InstanceConfigurationKey.ENABLE_SIGNUP) {
                        result.put(key, "1");
                    } else if (key == InstanceConfigurationKey.IS_GITHUB_ENABLED) {
                        result.put(key, "syncturtle-dev");
                    } else {
                        result.put(key, "0");
                    }
                }

                return result;
            });
            when(configurationApiMapper.toResponse(anyMap(), same(publicUrls))).thenReturn(configResponse);
            when(userRepository.countByActiveTrue()).thenReturn(3L);
            when(instanceApiMapper.toInstanceResponse(same(instance), eq(3L), eq(false)))
                    .thenReturn(instanceResponse);
            // act
            InstanceSetupResponse actual = service.getPublicInstance();
            // assert
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
            // verify
            verify(resolver).resolveRequested(requestedKeysCaptor.capture());
            assertThat(requestedKeysCaptor.getValue())
                    .extracting(RequestedKeyParam::getKey)
                    .contains(
                            InstanceConfigurationKey.ENABLE_SIGNUP,
                            InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION,
                            InstanceConfigurationKey.IS_GITHUB_ENABLED,
                            InstanceConfigurationKey.GITHUB_APP_NAME,
                            InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD,
                            InstanceConfigurationKey.INTERCOM_APP_ID);
            verify(configurationApiMapper).toResponse(
                    argThat(config -> "1"
                            .equals(config.get(InstanceConfigurationKey.ENABLE_SIGNUP))),
                    same(publicUrls));
            verify(userRepository).countByActiveTrue();
            verify(instanceApiMapper).toInstanceResponse(same(instance), eq(3L), eq(false));
            verifyNoInteractions(outboxWriter);
        }

    }

    @Nested
    @DisplayName("instanceUpdate(InstanceUpdateRequest)")
    class instanceUpdateTests {

        @Test
        @DisplayName("rejects null request before hitting database")
        void rejectsNullRequestBeforeHittingDb() {
            // arrange
            // conditions
            // act
            assertThatThrownBy(() -> service.instanceUpdate(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("request is required");
            // assert
            // verify
            verifyNoInteractions(instanceRepository, userRepository, resolver, outboxWriter, instanceApiMapper,
                    configurationApiMapper);
        }

        @Test
        @DisplayName("throws BAD_REQUEST when instance is not configured")
        void throwsBadRequestWhenInstanceIsNotConfigured() {
            // arrange
            InstanceUpdateRequest request = InstanceUpdateRequest.builder()
                    .instanceName("Syncturtle")
                    .build();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.empty());
            // act
            ResponseStatusException ex = catchThrowableOfType(
                    ResponseStatusException.class, () -> service.instanceUpdate(request));
            // assert
            assertThat(ex).isNotNull();
            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getReason()).isEqualTo("Instance is not configured.");
            // verify
            verify(instanceRepository).findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class);
            verify(instanceRepository, never()).saveAndFlush(any());
            verifyNoInteractions(userRepository, resolver, outboxWriter, instanceApiMapper,
                    configurationApiMapper);
        }

        @Test
        @DisplayName("renames instance, saves, publishes event, counts users, and maps response")
        void renamesSavesPublishesCountsAndMaps() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Old Name");
            InstanceUpdateRequest request = InstanceUpdateRequest.builder()
                    .instanceName(" New Name ")
                    .build();
            InstanceResponse expected = ResponseFixtures.instanceResponse("New Name", 2L);
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceRepository.saveAndFlush(any(Instance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(eventFactory.updated(instance)).thenReturn(InstanceEventFixtures.instanceUpdate());
            when(userRepository.countByActiveTrue()).thenReturn(2L);
            when(instanceApiMapper.toInstanceResponse(
                    argThat(saved -> "New Name".equals(saved.getInstanceName())),
                    eq(2L), eq(false))).thenReturn(expected);
            // act
            InstanceResponse actual = service.instanceUpdate(request);
            // assert
            assertThat(actual).isSameAs(expected);
            assertThat(instance.getInstanceName()).isEqualTo("New Name");
            // verify
            verify(instanceRepository).saveAndFlush(same(instance));
            verify(outboxWriter).saveInstanceEvent(eventCaptor.capture());
            assertThatPublishedEvent(eventCaptor.getValue())
                    .isInstanceUpdated()
                    .hasInstanceId(instance.getId())
                    .occurredAt(TestClocks.NOW);
            // .hasSetupDone(instance.isSetupDone());

            InOrder order = inOrder(instanceRepository, outboxWriter, userRepository, instanceApiMapper);
            order.verify(instanceRepository).findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class);
            order.verify(instanceRepository).saveAndFlush(same(instance));
            order.verify(outboxWriter).saveInstanceEvent(any(InstanceEvent.class));
            order.verify(userRepository).countByActiveTrue();
            order.verify(instanceApiMapper).toInstanceResponse(same(instance), eq(2L), eq(false));

            verifyNoInteractions(resolver, configurationApiMapper);
        }

        @Test
        @DisplayName("can use doAnswer() for void publisher methods")
        void canUseDoAnswerForVoidPublisherMethods() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Old Name");
            InstanceUpdateRequest request = InstanceUpdateRequest.builder()
                    .instanceName(" New Name ")
                    .build();
            InstanceResponse expected = ResponseFixtures.instanceResponse("New Name", 1L);
            AtomicInteger publishCount = new AtomicInteger();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceRepository.saveAndFlush(any(Instance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(eventFactory.updated(instance)).thenReturn(InstanceEventFixtures.instanceUpdate());
            doAnswer(invocation -> {
                publishCount.incrementAndGet();
                return null;
            }).when(outboxWriter).saveInstanceEvent(any(InstanceEvent.class));
            when(userRepository.countByActiveTrue()).thenReturn(1L);
            when(instanceApiMapper.toInstanceResponse(any(Instance.class), eq(1L), eq(false)))
                    .thenReturn(expected);
            // act
            InstanceResponse actual = service.instanceUpdate(request);
            // assert
            assertThat(actual).isSameAs(expected);
            assertThat(publishCount).hasValue(1);
        }

        @Test
        @DisplayName("bubbles up publisher failure after save")
        void bubblesUpPublisherFailureAfterSave() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Old Name");
            InstanceUpdateRequest request = InstanceUpdateRequest.builder()
                    .instanceName(" New Name ")
                    .build();
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceRepository.saveAndFlush(any(Instance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(eventFactory.updated(instance)).thenReturn(InstanceEventFixtures.instanceUpdate());
            doThrow(new IllegalStateException("outbox unavailable")).when(outboxWriter)
                    .saveInstanceEvent(any(InstanceEvent.class));
            // act + assert
            assertThatThrownBy(() -> service.instanceUpdate(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("outbox unavailable");
            // verify
            verify(instanceRepository).saveAndFlush(same(instance));
            verifyNoInteractions(userRepository, instanceApiMapper, resolver, configurationApiMapper);
        }

    }

    @Nested
    @DisplayName("markSignupScreenVisited()")
    class markSignupScreenVisitedTests {

        @Test
        @DisplayName("marks signup screen visited, saves, and publishes update")
        void marksSignupScreenVisitedSavesAndPublishesUpdate() {
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceRepository.saveAndFlush(any(Instance.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(eventFactory.updated(instance)).thenReturn(InstanceEventFixtures.instanceUpdate());
            // act
            service.markSignupScreenVisited();
            // assert
            assertThat(instance.isSignupScreenVisited()).isTrue();
            // verify
            verify(instanceRepository).saveAndFlush(same(instance));
            verify(outboxWriter).saveInstanceEvent(eventCaptor.capture());
            assertThatPublishedEvent(eventCaptor.getValue())
                    .isInstanceUpdated()
                    .hasInstanceId(instance.getId())
                    .occurredAt(TestClocks.NOW);
            verifyNoInteractions(userRepository, resolver, instanceApiMapper, configurationApiMapper);
        }

        @Test
        @DisplayName("throws BAD_REQUEST when instance is missing")
        void throwsBadRequestWhenInstanceIsMissing() {
            // arrange
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.empty());
            // act + assert
            assertThatThrownBy(() -> service.markSignupScreenVisited())
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> assertThat(
                            ((ResponseStatusException) error).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
            // verify
            verify(instanceRepository, never()).saveAndFlush(any());
            verifyNoInteractions(outboxWriter, userRepository, resolver, instanceApiMapper,
                    configurationApiMapper);
        }

    }

}
