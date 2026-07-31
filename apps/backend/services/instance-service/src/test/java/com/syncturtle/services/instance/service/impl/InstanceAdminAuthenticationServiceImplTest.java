package com.syncturtle.services.instance.service.impl;

import static com.syncturtle.services.instance.support.assertion.PublishedInstanceEventAssert.assertThatPublishedEvent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.syncturtle.common.contracts.auth.session.IssueInstanceAdminSessionRequest;
import com.syncturtle.common.contracts.auth.session.IssueSessionResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSigninRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSigninResponse;
import com.syncturtle.common.contracts.instance.admin.AdminSignupRequest;
import com.syncturtle.common.contracts.instance.admin.AdminSignupResponse;
import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.web.context.RequestClientContext;
import com.syncturtle.common.web.property.PublicUrlProperties;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.instance.client.UserClient;
import com.syncturtle.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.services.instance.dto.response.InstanceAdminAuthResponse;
import com.syncturtle.services.instance.messaging.kafka.factory.InstanceEventFactory;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceAdmin;
import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.InstanceAdminAuthenticationService;
import com.syncturtle.services.instance.service.collaborator.outbox.InstanceOutboxWriter;
import com.syncturtle.services.instance.support.clock.TestClocks;
import com.syncturtle.services.instance.support.fixture.InstanceEventFixtures;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;
import com.syncturtle.services.instance.support.fixture.PublicUrlFixtures;
import com.syncturtle.services.instance.support.fixture.RequestFixtures;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("InstanceAdminAuthenticationService")
class InstanceAdminAuthenticationServiceImplTest {

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
    ArgumentCaptor<IssueInstanceAdminSessionRequest> issueSessionRequestCaptor;
    @Captor
    ArgumentCaptor<InstanceEvent> eventCaptor;

    private PublicUrlProperties publicUrls;
    private PublicUrlResolver hostResolver;
    private InstanceAdminAuthenticationService service;

    @BeforeEach
    void setup() {
        publicUrls = PublicUrlFixtures.publicUrls();
        hostResolver = new PublicUrlResolver(publicUrls);
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
    @DisplayName("instanceAdminSignup(InstanceAdminSignupForm)")
    class InstanceAdminSignupTests {

        @Test
        @DisplayName("rejects null form before hitting deps")
        void rejectsNullFormBeforeHittingDeps() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.instanceAdminSignup(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("signup form is required");
            // verify
            verifyNoInteractions(instanceRepository, instanceAdminRepository, userClient, outboxWriter,
                    transactionTemplate,
                    requestClientContext);
        }

        @Test
        @DisplayName("creates first admin, completes instance setup, publishes update, issues session, and returns success redirect")
        void createsFirstAdminCompletesSetupPublishesUpdateIssuesSessionAndReturnsSuccessRedirect() {
            // arrange
            UUID userId = UUID.randomUUID();
            long userAuthVersion = 4L;
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");

            InstanceAdminSignupForm form = RequestFixtures.validSignupForm();

            AdminSignupResponse adminSignupResponse = mock(AdminSignupResponse.class);
            IssueSessionResponse issueSessionResponse = mock(IssueSessionResponse.class);
            // conditions
            runTransactionCallbacks();

            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.existsByDeletedAtIsNull()).thenReturn(false, false);
            when(requestClientContext.getClientIp()).thenReturn("127.0.0.1");
            when(requestClientContext.getUserAgent()).thenReturn("JUnit");
            when(userClient.adminSignupPost(any(AdminSignupRequest.class))).thenReturn(adminSignupResponse);
            when(adminSignupResponse.getUserId()).thenReturn(userId);
            when(adminSignupResponse.getAuthVersion()).thenReturn(userAuthVersion);
            when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
            when(instanceAdminRepository.saveAndFlush(any(InstanceAdmin.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(eventFactory.updated(instance)).thenReturn(InstanceEventFixtures.instanceUpdate());
            when(instanceRepository.saveAndFlush(same(instance))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userClient.issueInstanceAdminSessionPost(any(IssueInstanceAdminSessionRequest.class)))
                    .thenReturn(issueSessionResponse);
            // act
            InstanceAdminAuthResponse actual = service.instanceAdminSignup(form);
            // assert
            assertThat(actual.getRedirection()).isEqualTo(hostResolver.admin("/general"));
            assertThat(actual.getSession()).isSameAs(issueSessionResponse);
            assertThat(instance.isSetupDone()).isTrue();
            assertThat(instance.getInstanceName()).isEqualTo("Syncturtle");
            // verify
            verify(userClient).adminSignupPost(signupRequestCaptor.capture());
            AdminSignupRequest signupRequest = signupRequestCaptor.getValue();
            assertThat(signupRequest.getFirstName()).isEqualTo("Luna");
            assertThat(signupRequest.getLastName()).isEqualTo("Snow");
            assertThat(signupRequest.getEmail()).isEqualTo("lunasnow@marvel.com");
            assertThat(signupRequest.getPassword()).isEqualTo("secret-password");
            assertThat(signupRequest.getCompanyName()).isEqualTo("Syncturtle");
            assertThat(signupRequest.isTelemetryEnabled()).isTrue();
            assertThat(signupRequest.getUserAgent()).isEqualTo("JUnit");
            assertThat(signupRequest.getClientIp()).isEqualTo("127.0.0.1");

            verify(instanceAdminRepository).saveAndFlush(any(InstanceAdmin.class));
            verify(instanceRepository).saveAndFlush(same(instance));

            verify(outboxWriter).saveInstanceEvent(eventCaptor.capture());
            assertThatPublishedEvent(eventCaptor.getValue())
                    .isInstanceUpdated()
                    .hasInstanceId(instance.getId())
                    .occurredAt(TestClocks.NOW)
                    .hasSetupDone(true);

            verify(userClient).issueInstanceAdminSessionPost(issueSessionRequestCaptor.capture());
            IssueInstanceAdminSessionRequest sessionRequest = issueSessionRequestCaptor.getValue();

            assertThat(sessionRequest.getUserId()).isEqualTo(userId);
            assertThat(sessionRequest.getInstanceId()).isEqualTo(instance.getId());
            assertThat(sessionRequest.getUserAuthVersion()).isEqualTo(userAuthVersion);
            assertThat(sessionRequest.getClientIp()).isEqualTo("127.0.0.1");
            assertThat(sessionRequest.getUserAgent()).isEqualTo("JUnit");
        }

        @Test
        @DisplayName("returns error redirect when instance is not configured")
        void returnsErrorRedirectWhenInstanceIsNotConfigured() {
            // arrange
            InstanceAdminSignupForm form = RequestFixtures.validSignupForm();
            // conditions
            runTransactionCallbacks();
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.empty());
            // act
            InstanceAdminAuthResponse actual = service.instanceAdminSignup(form);
            // assert
            assertThat(actual.getSession()).isNull();
            assertThat(actual.getRedirection()).startsWith(hostResolver.admin(""));
            // verify
            verify(instanceRepository).findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class);
            verifyNoInteractions(userClient, outboxWriter, requestClientContext);
            ;
            verify(instanceAdminRepository, never()).saveAndFlush(any(InstanceAdmin.class));
            verify(instanceRepository, never()).saveAndFlush(any(Instance.class));
        }

        @Test
        @DisplayName("returns error redirect when an admin already exists")
        void returnsErrorRedirectWhenAdminAlreadyExists() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            InstanceAdminSignupForm form = RequestFixtures.validSignupForm();
            // conditions
            runTransactionCallbacks();
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(instanceAdminRepository.existsByDeletedAtIsNull()).thenReturn(true);
            // act
            InstanceAdminAuthResponse actual = service.instanceAdminSignup(form);
            // assert
            assertThat(actual.getSession()).isNull();
            assertThat(actual.getRedirection()).startsWith(hostResolver.admin(""));
            // verify
            verify(instanceAdminRepository).existsByDeletedAtIsNull();
            verifyNoInteractions(userClient, outboxWriter, requestClientContext);
            verify(instanceAdminRepository, never()).saveAndFlush(any(InstanceAdmin.class));
            verify(instanceRepository, never()).saveAndFlush(any(Instance.class));
        }

    }

    @Nested
    @DisplayName("instanceAdminSignin(InstanceAdminSigninForm)")
    class InstanceAdminSigninTests {

        @Test
        @DisplayName("rejects null form before hitting dependencies")
        void rejectsNullFormBeforeHittingDependencies() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> service.instanceAdminSignin(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("signin form is required");
            // verify
            verifyNoInteractions(instanceRepository, instanceAdminRepository, userClient, outboxWriter,
                    transactionTemplate,
                    requestClientContext);
        }

        @Test
        @DisplayName("signs in existing admin, issues session, and returns success redirect")
        void signsInExistingAdminIssuesSessionAndReturnsSuccessRedirect() {
            // arrange
            UUID userId = UUID.randomUUID();
            long userAuthVersion = 8L;

            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            InstanceAdmin instanceAdmin = InstanceAdmin.createInitialOwner(userId, instance);
            InstanceAdminSigninForm form = RequestFixtures.validSigninForm();

            AdminSigninResponse adminSigninResponse = mock(AdminSigninResponse.class);
            IssueSessionResponse issueSessionResponse = mock(IssueSessionResponse.class);
            // conditions
            runTransactionCallbacks();
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(requestClientContext.getClientIp()).thenReturn("127.0.0.1");
            when(requestClientContext.getUserAgent()).thenReturn("JUnit");
            when(userClient.adminSigninPost(any(AdminSigninRequest.class))).thenReturn(adminSigninResponse);
            when(adminSigninResponse.getUserId()).thenReturn(userId);
            when(adminSigninResponse.getAuthVersion()).thenReturn(userAuthVersion);
            when(instanceAdminRepository.findByInstance_IdAndUserId(instance.getId(), userId))
                    .thenReturn(Optional.of(instanceAdmin));
            when(userClient.issueInstanceAdminSessionPost(any(IssueInstanceAdminSessionRequest.class)))
                    .thenReturn(issueSessionResponse);
            // act
            InstanceAdminAuthResponse actual = service.instanceAdminSignin(form);
            // assert
            assertThat(actual.getRedirection()).isEqualTo(hostResolver.admin("/general"));
            assertThat(actual.getSession()).isSameAs(issueSessionResponse);
            // verify
            verify(userClient).adminSigninPost(signinRequestCaptor.capture());
            AdminSigninRequest signinRequest = signinRequestCaptor.getValue();

            assertThat(signinRequest.getEmail()).isEqualTo("lunasnow@marvel.com");
            assertThat(signinRequest.getPassword()).isEqualTo("secret-password");
            assertThat(signinRequest.getClientIp()).isEqualTo("127.0.0.1");
            assertThat(signinRequest.getUserAgent()).isEqualTo("JUnit");

            verify(userClient).issueInstanceAdminSessionPost(issueSessionRequestCaptor.capture());
            IssueInstanceAdminSessionRequest sessionRequest = issueSessionRequestCaptor.getValue();

            assertThat(sessionRequest.getUserId()).isEqualTo(userId);
            assertThat(sessionRequest.getInstanceId()).isEqualTo(instance.getId());
            assertThat(sessionRequest.getUserAuthVersion()).isEqualTo(userAuthVersion);
            assertThat(sessionRequest.getClientIp()).isEqualTo("127.0.0.1");
            assertThat(sessionRequest.getUserAgent()).isEqualTo("JUnit");

            verifyNoInteractions(outboxWriter);
        }

        @Test
        @DisplayName("returns error redirect when signed-in user is not an instance admin")
        void returnsErrorRedirectWhenSignedInUserIsNotInstanceAdmin() {
            // arrange
            UUID userId = UUID.randomUUID();
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");

            InstanceAdminSigninForm form = RequestFixtures.validSigninForm();
            AdminSigninResponse adminSigninResponse = mock(AdminSigninResponse.class);
            // conditions
            runTransactionCallbacks();
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(requestClientContext.getClientIp()).thenReturn("127.0.0.1");
            when(requestClientContext.getUserAgent()).thenReturn("JUnit");
            when(userClient.adminSigninPost(any(AdminSigninRequest.class))).thenReturn(adminSigninResponse);
            when(adminSigninResponse.getUserId()).thenReturn(userId);
            when(instanceAdminRepository.findByInstance_IdAndUserId(instance.getId(), userId))
                    .thenReturn(Optional.empty());
            // act
            InstanceAdminAuthResponse actual = service.instanceAdminSignin(form);
            // assert
            assertThat(actual.getSession()).isNull();
            assertThat(actual.getRedirection()).startsWith(hostResolver.admin(""));
            // verify
            verify(userClient, never()).issueInstanceAdminSessionPost(any(IssueInstanceAdminSessionRequest.class));
            verifyNoInteractions(outboxWriter);
        }

        @Test
        @DisplayName("returns error redirect when user-service rejects credentials")
        void returnsErrorRedirectWhenUserServiceRejectsCredentials() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");

            InstanceAdminSigninForm form = RequestFixtures.validSigninForm();
            form.setPassword("bad-password");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(requestClientContext.getClientIp()).thenReturn("127.0.0.1");
            when(requestClientContext.getUserAgent()).thenReturn("JUnit");
            when(userClient.adminSigninPost(any(AdminSigninRequest.class)))
                    .thenThrow(AuthException.of(AuthErrorCode.ADMIN_AUTHENTICATION_FAILED));
            // act
            InstanceAdminAuthResponse actual = service.instanceAdminSignin(form);
            // assert
            assertThat(actual.getSession()).isNull();
            assertThat(actual.getRedirection()).startsWith(hostResolver.admin(""));
            // verify
            verifyNoInteractions(transactionTemplate, outboxWriter);
            verify(userClient, never()).issueInstanceAdminSessionPost(any(IssueInstanceAdminSessionRequest.class));
        }

    }

    @SuppressWarnings({ "rawtypes" })
    private void runTransactionCallbacks() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

}
