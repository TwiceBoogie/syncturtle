package com.syncturtle.platform.services.instance.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.core.enums.InstanceEdition;
import com.syncturtle.common.core.events.InstanceEvent;
import com.syncturtle.common.core.events.InstanceEvent.Type;
import com.syncturtle.common.core.exceptions.AuthenticationException;
import com.syncturtle.common.spring.web.url.HostUrlBuilder;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.common.web.dto.request.AdminSignupInternalRequest;
import com.syncturtle.common.web.dto.response.AdminSignupInternalResponse;
import com.syncturtle.platform.services.instance.client.UserClient;
import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSignupResult;
import com.syncturtle.platform.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.platform.services.instance.dto.request.InstanceRequest;
import com.syncturtle.platform.services.instance.models.Instance;
import com.syncturtle.platform.services.instance.models.InstanceAdmin;
import com.syncturtle.platform.services.instance.models.User;
import com.syncturtle.platform.services.instance.payload.InstanceEventToPublish;
import com.syncturtle.platform.services.instance.payload.InstanceSummary;
import com.syncturtle.platform.services.instance.payload.InstanceSummaryWithConfig;
import com.syncturtle.platform.services.instance.repositories.InstanceAdminRepository;
import com.syncturtle.platform.services.instance.repositories.InstanceRepository;
import com.syncturtle.platform.services.instance.repositories.UserRepository;
import com.syncturtle.platform.services.instance.repositories.projections.InstanceOnlyIdProjection;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationResolver;
import com.syncturtle.platform.services.instance.services.impl.InstanceServiceImpl;

@ExtendWith(MockitoExtension.class)
public class InstanceServiceTest {

    @Mock
    InstanceRepository instanceRepository;
    @Mock
    InstanceAdminRepository instanceAdminRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    UserClient userClient;
    @Mock
    InstanceConfigurationResolver resolver;
    @Mock
    HostUrlBuilder hostResolver;
    @Mock
    RequestUserContext userContext;
    @Mock
    ApplicationEventPublisher events;

    @InjectMocks
    InstanceServiceImpl service;

    @Nested
    class InstanceInfoAndConfigTests {

        @Test
        void whenInstanceNotConfigured_returnEmpty_andDontHitResolver() {
            // condition
            when(instanceRepository.findTopByOrderByCreatedAtDesc(Instance.class)).thenReturn(Optional.empty());
            // result
            Optional<InstanceSummaryWithConfig> result = service.instanceInfoAndConfig();
            // assertions
            assertThat(result).isEmpty();
            // verify
            verify(instanceRepository).findTopByOrderByCreatedAtDesc(Instance.class);
            verifyNoInteractions(resolver, userRepository);
            verifyNoMoreInteractions(instanceRepository);
        }

        @Test
        void whenInstanceExists_returnsAggregate() {
            // arrange
            Instance instance = mock(Instance.class);
            // condition
            when(instanceRepository.findTopByOrderByCreatedAtDesc(Instance.class)).thenReturn(Optional.of(instance));
            when(resolver.resolveRequested(anyList())).thenReturn(Map.of());
            when(userRepository.count()).thenReturn(42L);
            // act
            Optional<InstanceSummaryWithConfig> result = service.instanceInfoAndConfig();
            // assertions
            assertThat(result).isPresent();
            assertThat(result.get().getInstance()).isEqualTo(instance);
            assertThat(result.get().getUserCount()).isEqualTo(42L);
            // verify
            verify(instanceRepository).findTopByOrderByCreatedAtDesc(Instance.class);
            verify(resolver).resolveRequested(anyList());
            verify(userRepository).count();
        }

        @Test
        void requestsCorrectConfigKeys() {
            // arrange
            Instance instance = mock(Instance.class);
            // conditions
            when(instanceRepository.findTopByOrderByCreatedAtDesc(Instance.class)).thenReturn(Optional.of(instance));
            when(userRepository.count()).thenReturn(1L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<InstanceConfigurationResolver.RequestedKey>> captor = ArgumentCaptor
                    .forClass(List.class);

            when(resolver.resolveRequested(captor.capture())).thenReturn(Map.of());
            // act
            service.instanceInfoAndConfig();

            List<InstanceConfigurationResolver.RequestedKey> requested = captor.getValue();
            // assert
            assertThat(requested).isNotEmpty();
            assertThat(requested)
                    .anyMatch(k -> k.key() == InstanceConfigurationKey.ENABLE_SIGNUP)
                    .anyMatch(k -> k.key() == InstanceConfigurationKey.POSTHOG_HOST);
        }

    }

    @Nested
    class instanceUpdateTests {

        @Test
        void whenInstanceMissing_throwBadRequest_andDontPublishEvent() {
            // arrange
            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
            // act + assert
            assertThatThrownBy(() -> service.instanceUpdate(new InstanceRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Instance is not registered yet.");

            // verify
            verify(instanceRepository).findFirstByOrderByCreatedAtDesc();
            verifyNoMoreInteractions(instanceRepository);
            verifyNoInteractions(userRepository);
            verifyNoInteractions(events);
        }

        @Test
        void instanceUpdate_updatesOnlyProvidedFields_publishesEvent_andReturnsSummary() {
            // arrange
            Instance instance = validInstanceEntity();

            InstanceRequest request = new InstanceRequest();
            request.setInstanceName("New Name");
            request.setTelemetryEnabled(null);

            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(instance));
            // service assigns instance = instanceRepository.saveAndFlush(instance)
            when(instanceRepository.saveAndFlush(any(Instance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.count()).thenReturn(99L);
            // act
            InstanceSummary result = service.instanceUpdate(request);
            // assert - returned summary
            assertThat(result.getInstance().getInstanceName()).isEqualTo("New Name");
            assertThat(result.getInstance().isTelemetryEnabled()).isFalse();
            assertThat(result.getUserCount()).isEqualTo(99L);

            // assert - saved instance
            ArgumentCaptor<Instance> savedCaptor = ArgumentCaptor.forClass(Instance.class);
            ArgumentCaptor<InstanceEventToPublish> publishedCaptor = ArgumentCaptor
                    .forClass(InstanceEventToPublish.class);

            InOrder order = inOrder(instanceRepository, events, userRepository);
            order.verify(instanceRepository).findFirstByOrderByCreatedAtDesc();
            order.verify(instanceRepository).saveAndFlush(savedCaptor.capture());
            order.verify(events).publishEvent(publishedCaptor.capture());
            order.verify(userRepository).count();

            // assert saved instance
            Instance saved = savedCaptor.getValue();
            assertThat(saved.getInstanceName()).isEqualTo("New Name");
            assertThat(saved.isTelemetryEnabled()).isFalse();

        }

        @Test
        void instanceUpdate_publishesInstanceUpdatedEvent() {
            // arrange
            Instance instance = validInstanceEntity();
            UUID instanceId = instance.getId();
            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(instance));
            when(instanceRepository.saveAndFlush(any(Instance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.count()).thenReturn(1L);
            // capture
            ArgumentCaptor<InstanceEventToPublish> publishedCaptor = ArgumentCaptor
                    .forClass(InstanceEventToPublish.class);
            // act
            Instant before = Instant.now();
            service.instanceUpdate(new InstanceRequest());
            Instant after = Instant.now();
            // verify
            verify(events).publishEvent(publishedCaptor.capture());
            // assert event payload
            InstanceEventToPublish wrapper = publishedCaptor.getValue();
            InstanceEvent event = wrapper.event();
            // assert event payload
            assertThat(event.getType()).isEqualTo(Type.INSTANCE_UPDATED);
            assertThat(event.getId()).isEqualTo(instanceId);
            assertThat(event.isSetupDone()).isTrue();
            assertThat(event.isTest()).isTrue();
            assertThat(event.getEdition()).isEqualTo(InstanceEdition.COMMUNITY);
            assertThat(event.getVersion()).isEqualTo(7L);
            assertThat(event.getOccurredAt()).isBetween(before, after);
        }

    }

    @Nested
    class GetInstanceAdminUserMeTests {

        @Test
        void readsUserContextAndFetchesFromRepo() {
            // arrange
            UUID userId = UUID.randomUUID();
            User user = mock(User.class);
            // conditions
            when(userContext.getUserId()).thenReturn(userId);
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            // act
            Optional<User> result = service.getInstanceAdminUserMe();
            // assert
            assertThat(result).isPresent().contains(user);
            // verify
            verify(userContext).getUserId();
            verify(userRepository).findById(userId);
        }

    }

    @Nested
    class GetInstanceAdminTests {

        @Test
        void whenNotRegistered_throws_Forbidden() {
            // conditions
            when(instanceRepository.findTopByOrderByCreatedAtDesc(InstanceOnlyIdProjection.class))
                    .thenReturn(Optional.empty());
            // result
            assertThatThrownBy(() -> service.getInstanceAdmins()).isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Instance is not registered yet");
            // assertions
            // assertThat(exception.getStatusCode().value()).isEqualTo(403);
            // assertThat(exception.getReason().contains("Instance is not registered yet"));
        }

    }

    @Nested
    class InstanceAdminSignupTests {

        @Test
        void whenInstanceNull_returnsRedirectWithErrors() {
            // conditoins
            when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
            when(hostResolver.buildAdminRedirectUriWithErrors(anyMap()))
                    .thenReturn("https://admin.syncturtle.com/god-mode/?error_message=INSTANCE_NOT_CONFIGURED");
            // act
            InstanceAdminSignupResult result = service.instanceAdminSignup(validFormStrongPassword());
            // assert
            assertThat(result.getUserId()).isNull();
            assertThat(result.getRedirectLocation()).contains("INSTANCE_NOT_CONFIGURED");
            // verify
            verify(instanceRepository).findFirstByOrderByCreatedAtDesc();
            verify(hostResolver).buildAdminRedirectUriWithErrors(anyMap());
            verifyNoInteractions(userClient);
        }

        @Test
        void whenAdminAlreadyExists_returnsRedirect() {
            // arrange
            Instance instance = mock(Instance.class);
            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(instance));
            when(instanceAdminRepository.existsByIdIsNotNull()).thenReturn(true);
            when(hostResolver.buildAdminRedirectUriWithErrors(anyMap()))
                    .thenReturn("https://admin.syncturtle.com/god-mode/?error_message=ADMIN_ALREADY_EXIST");
            // act
            InstanceAdminSignupResult result = service.instanceAdminSignup(validFormStrongPassword());
            // assertions
            assertThat(result.getUserId()).isNull();
            assertThat(result.getRedirectLocation()).contains("ADMIN_ALREADY_EXIST");
            // verify
            verify(instanceAdminRepository).existsByIdIsNotNull();
            verifyNoInteractions(userClient);
        }

        @Test
        void whenWeakPassword_returnsRedirect() {
            // arrange
            Instance instance = mock(Instance.class);
            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(instance));
            when(instanceAdminRepository.existsByIdIsNotNull()).thenReturn(false);
            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(hostResolver.buildAdminRedirectUriWithErrors(anyMap()))
                    .thenReturn("https://admin.syncturtle.com/god-mode/?error_message=INVALID_ADMIN_PASSWORD");
            // act
            InstanceAdminSignupResult result = service.instanceAdminSignup(validFormWeakPassword());
            // assertions
            assertThat(result.getUserId()).isNull();
            assertThat(result.getRedirectLocation()).contains("INVALID_ADMIN_PASSWORD");
            // verify
            verifyNoInteractions(userClient);
        }

        @Test
        void whenFeignSuccess_persistsAdmin_andReturnsGeneralRedirect() {
            // arrange
            Instance instance = validInstanceEntity();
            UUID createdUserId = UUID.randomUUID();
            InstanceAdmin instanceAdmin = new InstanceAdmin();
            instanceAdmin.setUserId(createdUserId);
            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(instance));
            when(instanceAdminRepository.existsByIdIsNotNull()).thenReturn(false);
            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(userClient.adminSignupPost(any(AdminSignupInternalRequest.class)))
                    .thenReturn(new AdminSignupInternalResponse(createdUserId));
            when(instanceAdminRepository.save(any(InstanceAdmin.class))).thenReturn(instanceAdmin);
            when(instanceRepository.saveAndFlush(any(Instance.class))).thenAnswer(inv -> inv.getArgument(0));
            when(hostResolver.adminHost()).thenReturn("https://admin.syncturtle.com/god-mode");
            // act
            InstanceAdminSignupResult result = service.instanceAdminSignup(validFormStrongPassword());
            assertThat(result.getUserId()).isEqualTo(createdUserId);
            assertThat(result.getRedirectLocation()).isEqualTo("https://admin.syncturtle.com/god-mode/general");
            // verify
            verify(userClient).adminSignupPost(any(AdminSignupInternalRequest.class));
            verify(instanceAdminRepository).save(any(InstanceAdmin.class));
            verify(instanceRepository).saveAndFlush(instance);
        }

        @Test
        void whenFeignThrowsAuthenticationException_propagatesOrHandles() {
            // arrange
            Instance instance = mock(Instance.class);
            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(instance));
            when(instanceAdminRepository.existsByIdIsNotNull()).thenReturn(false);
            when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
            when(userClient.adminSignupPost(any()))
                    .thenThrow(new AuthenticationException(AuthErrorCode.ADMIN_AUTHENTICATION_FAILED));
            when(hostResolver.buildAdminRedirectUriWithErrors(anyMap()))
                    .thenReturn("https://admin.syncturtle.com/god-mode/?error_message=ADMIN_AUTHENTICATION_FAILED");
            // act
            InstanceAdminSignupResult result = service.instanceAdminSignup(validFormStrongPassword());
            // assertions
            assertThat(result.getUserId()).isNull();
            assertThat(result.getRedirectLocation()).contains("ADMIN_AUTHENTICATION_FAILED");
            // verify
        }

    }

    private static InstanceAdminSignupForm validFormStrongPassword() {
        InstanceAdminSignupForm form = new InstanceAdminSignupForm();
        form.setFirstName("Sal");
        form.setLastName("Sebastian");
        form.setEmail("admin@example.com");
        form.setCompanyName("SyncTurtle");
        form.setTelemetryEnabled("true");
        form.setPassword("VeryStrongPassword!2026#OK");
        return form;
    }

    private static InstanceAdminSignupForm validFormWeakPassword() {
        InstanceAdminSignupForm form = new InstanceAdminSignupForm();
        form.setFirstName("Sal");
        form.setLastName("Sebastian");
        form.setEmail("admin@example.com");
        form.setCompanyName("SyncTurtle");
        form.setTelemetryEnabled("true");
        form.setPassword("1234");
        return form;
    }

    private static Instance validInstanceEntity() {
        Instance instance = new Instance();
        UUID instanceId = UUID.randomUUID();
        instance.setId(instanceId);
        instance.setInstanceName("Old");
        instance.setTelemetryEnabled(false);
        instance.setSetupDone(true);
        instance.setTest(true);
        instance.setEdition(InstanceEdition.COMMUNITY);
        instance.setVersion(7L);
        return instance;
    }

}
