package com.syncturtle.services.instance.unit;
// package com.syncturtle.platform.services.instance.unit;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyList;
// import static org.mockito.ArgumentMatchers.anyMap;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.times;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.verifyNoInteractions;
// import static org.mockito.Mockito.verifyNoMoreInteractions;
// import static org.mockito.Mockito.when;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.junit.Assert.assertThrows;

// import java.util.List;
// import java.util.Map;
// import java.util.Optional;
// import java.util.UUID;

// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.web.server.ResponseStatusException;

// import com.syncturtle.common.core.enums.InstanceConfigurationKey;
// import com.syncturtle.common.spring.web.url.HostUrlBuilder;
// import com.syncturtle.common.web.context.RequestUserContext;
// import com.syncturtle.common.web.dto.response.AdminSignupInternalResponse;
// import com.syncturtle.platform.services.instance.client.UserClient;
// import
// com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSignupResult;
// import
// com.syncturtle.platform.services.instance.dto.request.InstanceAdminSignupForm;
// import com.syncturtle.platform.services.instance.models.Instance;
// import com.syncturtle.platform.services.instance.models.User;
// import
// com.syncturtle.platform.services.instance.models.readmodel.InstanceInfoRow;
// import
// com.syncturtle.platform.services.instance.repositories.InstanceAdminRepository;
// import
// com.syncturtle.platform.services.instance.repositories.InstanceInfoAggregate;
// import
// com.syncturtle.platform.services.instance.repositories.InstanceRepository;
// import com.syncturtle.platform.services.instance.repositories.UserRepository;
// import
// com.syncturtle.platform.services.instance.repositories.projections.InstanceOnlyIdProjection;
// import
// com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationResolver;
// import
// com.syncturtle.platform.services.instance.services.impl.InstanceServiceImpl;

// @ExtendWith(MockitoExtension.class)
// public class InstanceServiceTest {

// @Mock
// InstanceRepository instanceRepository;
// @Mock
// InstanceAdminRepository instanceAdminRepository;
// @Mock
// UserRepository userRepository;
// @Mock
// UserClient userClient;
// @Mock
// InstanceConfigurationResolver resolver;
// @Mock
// HostUrlBuilder hostResolver;
// @Mock
// RequestUserContext userContext;

// @InjectMocks
// InstanceServiceImpl service;

// @Test
// void
// instanceInfoAndConfig_whenInstanceNotConfigured_returnEmpty_andDontHitResolver()
// {
// // condition
// when(instanceRepository.findLatestInfoRow()).thenReturn(Optional.empty());
// // result
// Optional<InstanceInfoAggregate> result = service.instanceInfoAndConfig();
// // assertions
// assertThat(result).isEmpty();
// // verify
// verify(instanceRepository).findLatestInfoRow();
// verifyNoInteractions(resolver, userRepository);
// verifyNoMoreInteractions(instanceRepository);
// }

// @Test
// void instanceInfoAndConfig_whenConfigured_returnsAggregate() {
// // arrange
// InstanceInfoRow row = mock(InstanceInfoRow.class);
// // condition
// when(instanceRepository.findLatestInfoRow()).thenReturn(Optional.of(row));
// when(resolver.resolveRequested(anyList())).thenReturn(Map.of());
// when(userRepository.count()).thenReturn(42L);
// // act
// Optional<InstanceInfoAggregate> result = service.instanceInfoAndConfig();
// // assert
// assertThat(result).isPresent();
// assertThat(result.get().getInstance()).isEqualTo(row);
// assertThat(result.get().getUserCount()).isEqualTo(42L);
// // verify interactions
// verify(instanceRepository).findLatestInfoRow();
// verify(resolver).resolveRequested(anyList());
// verify(userRepository).count();
// }

// @Test
// void instanceInfoAndConfig_requestsCorrectConfigKeys() {
// // arrange
// InstanceInfoRow row = mock(InstanceInfoRow.class);
// // conditions
// when(instanceRepository.findLatestInfoRow()).thenReturn(Optional.of(row));
// when(userRepository.count()).thenReturn(1L);

// @SuppressWarnings("unchecked")
// ArgumentCaptor<List<InstanceConfigurationResolver.RequestedKey>> captor =
// ArgumentCaptor.forClass(List.class);

// when(resolver.resolveRequested(captor.capture())).thenReturn(Map.of());
// // act
// service.instanceInfoAndConfig();

// List<InstanceConfigurationResolver.RequestedKey> requested =
// captor.getValue();
// // assert
// assertThat(requested).isNotEmpty();
// assertThat(requested)
// .anyMatch(k -> k.key() == InstanceConfigurationKey.ENABLE_SIGNUP)
// .anyMatch(k -> k.key() == InstanceConfigurationKey.POSTHOG_HOST);
// }

// @Test
// void getInstanceAdminUserMe_readsContextAndFetchesFromRepo() {
// UUID userID = UUID.randomUUID();
// User user = mock(User.class);
// // conditions
// when(userContext.getUserId()).thenReturn(userID);
// when(userRepository.findById(userID)).thenReturn(Optional.of(user));
// // act
// Optional<User> result = service.getInstanceAdminUserMe();
// // assert
// assertThat(result).isPresent().contains(user);
// // verify
// verify(userContext).getUserId();
// verify(userRepository).findById(userID);
// }

// @Test
// void getInstanceAdmin_whenNotRegistered_throws_Forbidden() {
// // conditions
// when(instanceRepository.findTopByOrderByCreatedAtDesc(InstanceOnlyIdProjection.class))
// .thenReturn(Optional.empty());
// // result
// ResponseStatusException exception =
// assertThrows(ResponseStatusException.class,
// () -> service.getInstanceAdmins());
// // assert
// assertThat(exception.getStatusCode().value()).isEqualTo(403);
// assertThat(exception.getReason()).contains("Instance is not registered yet");
// }

// @Test
// void
// instanceAdminSignup_whenInstanceNull_returnsRedirect_andDoesNotCallFeign() {
// // conditions
// when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.empty());
// when(hostResolver.buildAdminRedirectUriWithErrors(anyMap()))
// .thenReturn("https://admin.syncturtle.com/god-mode/?error_message=INSTANCE_NOT_CONFIGURED");
// // act
// InstanceAdminSignupResult result = service.instanceAdminSignup(validForm());
// // assert
// assertThat(result.getUserId()).isNull();
// assertThat(result.getRedirectLocation()).contains("INSTANCE_NOT_CONFIGURED");
// // verify
// verifyNoInteractions(userClient);
// }

// @Test
// void instanceAdminSignup_happyPath_callsFeignOnce_andPersistsAdmin() {
// // arrrange
// Instance instance = mock(Instance.class);

// UUID userId = UUID.randomUUID();
// // conditions
// when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(instance));
// when(instanceAdminRepository.existsByIdIsNotNull()).thenReturn(false);
// when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

// when(userClient.adminSignupPost(any())).thenReturn(new
// AdminSignupInternalResponse(userId));
// when(instanceAdminRepository.save(any())).thenAnswer(inv ->
// inv.getArgument(0));
// when(hostResolver.adminHost()).thenReturn("https://admin.syncturtle.com/god-mode");
// // act
// InstanceAdminSignupResult result = service.instanceAdminSignup(validForm());
// // assert
// assertThat(result.getUserId()).isEqualTo(userId);
// assertThat(result.getRedirectLocation()).isEqualTo("https://admin.syncturtle.com/god-mode/general");
// // verify
// verify(userClient, times(1)).adminSignupPost(any());
// verify(instanceAdminRepository, times(1)).save(any());
// verify(instanceRepository, times(1)).save(any());
// }

// @Test
// void instanceAdminSignup_updatesInstanceFieldsBeforeSaving() {
// // arrange
// Instance instance = new Instance();
// instance.setSetupDone(true);
// instance.setTelemetryEnabled(true);

// UUID userId = UUID.randomUUID();
// // conditions
// when(instanceRepository.findFirstByOrderByCreatedAtDesc()).thenReturn(Optional.of(instance));
// when(instanceAdminRepository.existsByIdIsNotNull()).thenReturn(false);
// when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);

// when(userClient.adminSignupPost(any())).thenReturn(new
// AdminSignupInternalResponse(userId));
// when(instanceAdminRepository.save(any())).thenAnswer(inv ->
// inv.getArgument(0));
// when(hostResolver.adminHost()).thenReturn("https://admin.syncturtle.com/god-mode");
// // capture
// ArgumentCaptor<Instance> instanceCaptor =
// ArgumentCaptor.forClass(Instance.class);
// // act
// service.instanceAdminSignup(validForm());
// // verify
// verify(instanceRepository).save(instanceCaptor.capture());
// Instance saved = instanceCaptor.getValue();
// // assert
// assertThat(saved.isSetupDone()).isTrue();
// assertThat(saved.isTelemetryEnabled()).isTrue();
// // assertThat(saved.getInstanceName()).isEqualTo("");
// }

// private InstanceAdminSignupForm validForm() {
// InstanceAdminSignupForm form = new InstanceAdminSignupForm();
// form.setFirstName("Sal");
// form.setLastName("Sebastian");
// form.setEmail("admin@example.com");
// form.setPassword("VeryStrongPassword!123456789");
// form.setCompanyName("My Company");
// form.setTelemetryEnabled("true");
// return form;
// }

// }
