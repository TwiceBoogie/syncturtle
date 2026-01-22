package com.syncturtle.platform.services.instance.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.common.core.enums.AuthErrorCode;
import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.core.exceptions.AuthenticationException;
import com.syncturtle.common.spring.web.url.HostUrlBuilder;
import com.syncturtle.common.web.context.RequestUserContext;
import com.syncturtle.common.web.dto.request.AdminSignupInternalRequest;
import com.syncturtle.common.web.dto.response.AdminSignupInternalResponse;
import com.syncturtle.platform.services.instance.client.UserClient;
import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSignupResult;
import com.syncturtle.platform.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.platform.services.instance.models.Instance;
import com.syncturtle.platform.services.instance.models.InstanceAdmin;
import com.syncturtle.platform.services.instance.models.User;
import com.syncturtle.platform.services.instance.models.readmodel.InstanceInfoRow;
import com.syncturtle.platform.services.instance.repositories.InstanceAdminRepository;
import com.syncturtle.platform.services.instance.repositories.InstanceInfoAggregate;
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

    @InjectMocks
    InstanceServiceImpl service;

    @Nested
    class InstanceInfoAndConfigTests {

        @Test
        void whenInstanceNotConfigured_returnEmpty_andDontHitResolver() {
            // condition
            when(instanceRepository.findLatestInfoRow()).thenReturn(Optional.empty());
            // result
            Optional<InstanceInfoAggregate> result = service.instanceInfoAndConfig();
            // assertions
            assertThat(result).isEmpty();
            // verify
            verify(instanceRepository).findLatestInfoRow();
            verifyNoInteractions(resolver, userRepository);
            verifyNoMoreInteractions(instanceRepository);
        }

        @Test
        void whenInstanceExists_returnsAggregate() {
            // arrange
            InstanceInfoRow row = mock(InstanceInfoRow.class);
            // condition
            when(instanceRepository.findLatestInfoRow()).thenReturn(Optional.of(row));
            when(resolver.resolveRequested(anyList())).thenReturn(Map.of());
            when(userRepository.count()).thenReturn(42L);
            // act
            Optional<InstanceInfoAggregate> result = service.instanceInfoAndConfig();
            // assertions
            assertThat(result).isPresent();
            assertThat(result.get().getInstance()).isEqualTo(row);
            assertThat(result.get().getUserCount()).isEqualTo(42L);
            // verify
            verify(instanceRepository).findLatestInfoRow();
            verify(resolver).resolveRequested(anyList());
            verify(userRepository).count();
        }

        @Test
        void requestsCorrectConfigKeys() {
            // arrange
            InstanceInfoRow row = mock(InstanceInfoRow.class);
            // conditions
            when(instanceRepository.findLatestInfoRow()).thenReturn(Optional.of(row));
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
            Instance instance = mock(Instance.class);
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
            when(hostResolver.adminHost()).thenReturn("https://admin.syncturtle.com/god-mode");
            // act
            InstanceAdminSignupResult result = service.instanceAdminSignup(validFormStrongPassword());
            assertThat(result.getUserId()).isEqualTo(createdUserId);
            assertThat(result.getRedirectLocation()).isEqualTo("https://admin.syncturtle.com/god-mode/general");
            // verify
            verify(userClient).adminSignupPost(any(AdminSignupInternalRequest.class));
            verify(instanceAdminRepository).save(any(InstanceAdmin.class));
            verify(instanceRepository).save(instance);
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

}
