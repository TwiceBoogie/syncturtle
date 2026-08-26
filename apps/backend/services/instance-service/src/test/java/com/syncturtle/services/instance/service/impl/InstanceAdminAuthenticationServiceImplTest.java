package com.syncturtle.services.instance.service.impl;

import static com.syncturtle.services.instance.support.fixture.RequestFixtures.validSigninForm;
import static com.syncturtle.services.instance.support.fixture.RequestFixtures.validSignupForm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.AdminSessionHandoffResponse;
import com.syncturtle.common.contracts.auth.session.CreateInstanceAdminSessionHandoffRequest;
import com.syncturtle.common.contracts.auth.session.PreAuthTransactionBinding;
import com.syncturtle.common.contracts.instance.admin.AdminSigninRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSigninResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSignupResponse;
import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.instance.client.UserClient;
import com.syncturtle.services.instance.dto.response.InstanceAdminAuthResponse;
import com.syncturtle.services.instance.messaging.kafka.factory.InstanceEventFactory;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceAdmin;
import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.InstanceAdminAuthenticationService;
import com.syncturtle.services.instance.service.collaborator.outbox.InstanceOutboxWriter;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;
import com.syncturtle.services.instance.support.fixture.PublicUrlFixtures;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("InstanceAdminAuthenticationService")
class InstanceAdminAuthenticationServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String PRE_AUTH_HASH = "a".repeat(64);

    @Mock
    InstanceRepository instanceRepository;
    @Mock
    InstanceAdminRepository instanceAdminRepository;
    @Mock
    UserClient userClient;
    @Mock
    InstanceOutboxWriter outboxWriter;
    @Mock
    InstanceEventFactory eventFactory;
    @Mock
    TransactionTemplate transactionTemplate;
    @Mock
    RequestClientContext requestClientContext;
    @Captor
    ArgumentCaptor<AdminSignupRequest> signupRequestCaptor;
    @Captor
    ArgumentCaptor<AdminSigninRequest> signinRequestCaptor;
    @Captor
    ArgumentCaptor<CreateInstanceAdminSessionHandoffRequest> handoffRequestCaptor;

    private PublicUrlResolver hostResolver;
    private InstanceAdminAuthenticationService service;

    @BeforeEach
    void setup() {
        hostResolver = new PublicUrlResolver(PublicUrlFixtures.publicUrls());
        service = new InstanceAdminAuthenticationServiceImpl(
                instanceRepository,
                instanceAdminRepository,
                userClient,
                outboxWriter,
                eventFactory,
                hostResolver,
                transactionTemplate,
                requestClientContext);
    }

    @Nested
    @DisplayName("instanceAdminSignup(form, binding)")
    class SignupTests {

        @Test
        @DisplayName("commits authoritative setup then requests one opaque handoff with current versions")
        void commitsSetupThenRequestsOneOpaqueHandoff() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("SyncTurtle");
            AdminSignupResponse userResponse = new AdminSignupResponse(USER_ID, 7L);
            InstanceEvent event = org.mockito.Mockito.mock(InstanceEvent.class);
            AdminSessionHandoffResponse handoff = handoff();
            // conditions
            runTransactionCallbacks();
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.existsByDeletedAtIsNull()).thenReturn(false, false);
            when(requestClientContext.getClientIp()).thenReturn("192.0.2.10");
            when(requestClientContext.getUserAgent()).thenReturn("Admin browser");
            when(userClient.adminSignupPost(any(AdminSignupRequest.class))).thenReturn(userResponse);
            when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
            when(instanceAdminRepository.saveAndFlush(any(InstanceAdmin.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(instanceRepository.saveAndFlush(same(instance))).thenReturn(instance);
            when(eventFactory.updated(instance)).thenReturn(event);
            when(userClient.createInstanceAdminSessionHandoff(any(CreateInstanceAdminSessionHandoffRequest.class)))
                    .thenReturn(handoff);
            // act
            InstanceAdminAuthResponse result = service.instanceAdminSignup(
                    validSignupForm(),
                    PreAuthTransactionBinding.fromHash(PRE_AUTH_HASH));
            // assert
            assertThat(result.getRedirection()).isEqualTo(hostResolver.api("/auth/admin/session"));
            assertThat(result.getHandoff()).isSameAs(handoff);
            assertThat(instance.isSetupDone()).isTrue();
            // verify
            verify(userClient).adminSignupPost(signupRequestCaptor.capture());
            assertThat(signupRequestCaptor.getValue().getClientIp()).isEqualTo("192.0.2.10");
            verify(outboxWriter).saveInstanceEvent(event);
            verify(userClient).createInstanceAdminSessionHandoff(handoffRequestCaptor.capture());
            CreateInstanceAdminSessionHandoffRequest request = handoffRequestCaptor.getValue();
            assertThat(request.getUserId()).isEqualTo(USER_ID);
            assertThat(request.getInstanceId()).isEqualTo(instance.getId());
            assertThat(request.getUserAuthVersion()).isEqualTo(7L);
            assertThat(request.getAdminSessionVersion()).isEqualTo(1L);
            assertThat(request.getPreAuthBinding().getValue()).isEqualTo(PRE_AUTH_HASH);
            assertThat(request.getClientIp()).isEqualTo("192.0.2.10");
            assertThat(request.getUserAgent()).isEqualTo("Admin browser");
        }

        @Test
        @DisplayName("returns failure redirect without user or handoff when instance is not configured")
        void returnsFailureWhenInstanceIsNotConfigured() {
            // arrange
            // conditions
            runTransactionCallbacks();
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.empty());
            // act
            InstanceAdminAuthResponse result = service.instanceAdminSignup(
                    validSignupForm(),
                    PreAuthTransactionBinding.fromHash(PRE_AUTH_HASH));
            // assert
            assertThat(result.getHandoff()).isNull();
            assertThat(result.getRedirection()).startsWith(hostResolver.admin(""));
            // verify
            verifyNoInteractions(userClient, outboxWriter, requestClientContext);
        }

    }

    @Nested
    @DisplayName("instanceAdminSignin(form, binding)")
    class SigninTests {

        @Test
        @DisplayName("authenticates and propagates authoritative administrator version into one handoff")
        void authenticatesAndPropagatesAuthoritativeVersion() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("SyncTurtle");
            InstanceAdmin instanceAdmin = InstanceAdmin.createInitialOwner(USER_ID, instance);
            AdminSigninResponse userResponse = new AdminSigninResponse(USER_ID, 8L);
            AdminSessionHandoffResponse handoff = handoff();
            // conditions
            runTransactionCallbacks();
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(requestClientContext.getClientIp()).thenReturn("192.0.2.10");
            when(requestClientContext.getUserAgent()).thenReturn("Admin browser");
            when(userClient.adminSigninPost(any(AdminSigninRequest.class))).thenReturn(userResponse);
            when(instanceAdminRepository.findByInstance_IdAndUserId(instance.getId(), USER_ID))
                    .thenReturn(Optional.of(instanceAdmin));
            when(userClient.createInstanceAdminSessionHandoff(any(CreateInstanceAdminSessionHandoffRequest.class)))
                    .thenReturn(handoff);
            // act
            InstanceAdminAuthResponse result = service.instanceAdminSignin(
                    validSigninForm(),
                    PreAuthTransactionBinding.fromHash(PRE_AUTH_HASH));
            // assert
            assertThat(result.getHandoff()).isSameAs(handoff);
            assertThat(result.getRedirection()).isEqualTo(hostResolver.api("/auth/admin/session"));
            // verify
            verify(userClient).adminSigninPost(signinRequestCaptor.capture());
            assertThat(signinRequestCaptor.getValue().getEmail()).isEqualTo("lunasnow@marvel.com");
            verify(userClient).createInstanceAdminSessionHandoff(handoffRequestCaptor.capture());
            assertThat(handoffRequestCaptor.getValue().getAdminSessionVersion())
                    .isEqualTo(instanceAdmin.getSessionVersion());
            assertThat(handoffRequestCaptor.getValue().getUserAuthVersion()).isEqualTo(8L);
            verifyNoInteractions(outboxWriter);
        }

        @Test
        @DisplayName("does not request handoff when authenticated user is not an instance administrator")
        void doesNotRequestHandoffForNonAdministrator() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("SyncTurtle");
            AdminSigninResponse userResponse = new AdminSigninResponse(USER_ID, 8L);
            // conditions
            runTransactionCallbacks();
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(requestClientContext.getClientIp()).thenReturn("192.0.2.10");
            when(requestClientContext.getUserAgent()).thenReturn("Admin browser");
            when(userClient.adminSigninPost(any(AdminSigninRequest.class))).thenReturn(userResponse);
            when(instanceAdminRepository.findByInstance_IdAndUserId(instance.getId(), USER_ID))
                    .thenReturn(Optional.empty());
            // act
            InstanceAdminAuthResponse result = service.instanceAdminSignin(
                    validSigninForm(),
                    PreAuthTransactionBinding.fromHash(PRE_AUTH_HASH));
            // assert
            assertThat(result.getHandoff()).isNull();
            assertThat(result.getRedirection()).startsWith(hostResolver.admin(""));
            // verify
            verify(userClient, never()).createInstanceAdminSessionHandoff(any());
        }

        @Test
        @DisplayName("does not enter local transaction or handoff when credential authentication fails")
        void doesNotContinueWhenCredentialAuthenticationFails() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("SyncTurtle");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(requestClientContext.getClientIp()).thenReturn("192.0.2.10");
            when(requestClientContext.getUserAgent()).thenReturn("Admin browser");
            when(userClient.adminSigninPost(any(AdminSigninRequest.class)))
                    .thenThrow(AuthException.of(AuthErrorCode.ADMIN_AUTHENTICATION_FAILED));
            // act
            InstanceAdminAuthResponse result = service.instanceAdminSignin(
                    validSigninForm(),
                    PreAuthTransactionBinding.fromHash(PRE_AUTH_HASH));
            // assert
            assertThat(result.getHandoff()).isNull();
            // verify
            verifyNoInteractions(transactionTemplate, outboxWriter);
            verify(userClient, never()).createInstanceAdminSessionHandoff(any());
        }

    }

    @SuppressWarnings({ "rawtypes" })
    private void runTransactionCallbacks() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    private static AdminSessionHandoffResponse handoff() {
        Instant issuedAt = Instant.parse("2026-08-21T15:00:00Z");
        return AdminSessionHandoffResponse.builder()
                .completionCode("33333333-3333-3333-3333-333333333333.opaque")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(30))
                .build();
    }

}
