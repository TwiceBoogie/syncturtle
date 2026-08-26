package com.syncturtle.services.user.service.impl;

import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.CLIENT_BINDING;
import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.INSTANCE_ID;
import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.PRE_AUTH_BINDING;
import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.USER_ID;
import static com.syncturtle.services.user.support.fixture.AdminSessionHandoffTestFixtures.receipt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.contracts.auth.session.AdminSessionHandoffResponse;
import com.syncturtle.common.contracts.auth.session.CreateInstanceAdminSessionHandoffRequest;
import com.syncturtle.common.contracts.auth.session.PreAuthTransactionBinding;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.ProfileRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.UserAdministrationInternalService;
import com.syncturtle.services.user.service.collaborator.outbox.UserOutboxWriter;
import com.syncturtle.services.user.service.collaborator.session.AdminSessionHandoffStore;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionClientFingerprint;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionClientFingerprintFactory;
import com.syncturtle.services.user.service.param.AdminSessionHandoffCreateParam;
import com.syncturtle.services.user.support.clock.TestClocks;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("UserAdministrationInternalService handoff creation")
class UserAdministrationInternalServiceSessionTest {

    @Mock
    UserRepository userRepository;
    @Mock
    ProfileRepository profileRepository;
    @Mock
    AdminSessionHandoffStore handoffStore;
    @Mock
    RefreshSessionClientFingerprintFactory fingerprintFactory;
    @Mock
    UserEventFactory userEventFactory;
    @Mock
    UserOutboxWriter outboxWriter;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    User user;
    @Captor
    ArgumentCaptor<AdminSessionHandoffCreateParam> createParamCaptor;

    private UserAdministrationInternalService service;

    @BeforeEach
    void setup() {
        service = new UserAdministrationInternalServiceImpl(
                userRepository,
                profileRepository,
                handoffStore,
                fingerprintFactory,
                userEventFactory,
                outboxWriter,
                passwordEncoder,
                TestClocks.fixedUtc());
    }

    @Nested
    @DisplayName("createInstanceAdminSessionHandoff(request)")
    class CreateInstanceAdminSessionHandoffTests {

        @Test
        @DisplayName("derives fingerprint and maps internal receipt to opaque shared response")
        void derivesFingerprintAndMapsInternalReceipt() {
            // arrange
            CreateInstanceAdminSessionHandoffRequest request = request(7L);
            RefreshSessionClientFingerprint fingerprint = new RefreshSessionClientFingerprint(
                    "Admin browser", CLIENT_BINDING);
            // conditions
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(user.isLoginAllowed()).thenReturn(true);
            when(user.getAuthVersion()).thenReturn(7L);
            when(fingerprintFactory.create("192.0.2.10", "Admin browser")).thenReturn(fingerprint);
            when(handoffStore.create(any(AdminSessionHandoffCreateParam.class))).thenReturn(receipt());
            // act
            AdminSessionHandoffResponse result = service.createInstanceAdminSessionHandoff(request);
            // assert
            assertThat(result.getCompletionCode()).isEqualTo(receipt().getCompletionCode());
            assertThat(result.getIssuedAt()).isEqualTo(receipt().getIssuedAt());
            assertThat(result.getExpiresAt()).isEqualTo(receipt().getExpiresAt());
            // verify
            verify(fingerprintFactory).create("192.0.2.10", "Admin browser");
            verify(handoffStore).create(createParamCaptor.capture());
            AdminSessionHandoffCreateParam param = createParamCaptor.getValue();
            assertThat(param.getUserId()).isEqualTo(USER_ID);
            assertThat(param.getInstanceId()).isEqualTo(INSTANCE_ID);
            assertThat(param.getUserAuthVersion()).isEqualTo(7L);
            assertThat(param.getAdminSessionVersion()).isEqualTo(9L);
            assertThat(param.getPreAuthBindingHash()).isEqualTo(PRE_AUTH_BINDING);
            assertThat(param.getClientBindingHash()).isEqualTo(CLIENT_BINDING);
        }

        @Test
        @DisplayName("rejects a login-disallowed user before fingerprint or Redis")
        void rejectsLoginDisallowedUser() {
            // arrange
            CreateInstanceAdminSessionHandoffRequest request = request(7L);
            // conditions
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(user.isLoginAllowed()).thenReturn(false);
            // act + assert
            assertThatThrownBy(() -> service.createInstanceAdminSessionHandoff(request))
                    .isInstanceOf(AuthException.class);
            // verify
            verifyNoInteractions(fingerprintFactory, handoffStore);
        }

        @Test
        @DisplayName("rejects auth-version mismatch before fingerprint or Redis")
        void rejectsAuthVersionMismatch() {
            // arrange
            CreateInstanceAdminSessionHandoffRequest request = request(7L);
            // conditions
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(user.isLoginAllowed()).thenReturn(true);
            when(user.getAuthVersion()).thenReturn(8L);
            // act + assert
            assertThatThrownBy(() -> service.createInstanceAdminSessionHandoff(request))
                    .isInstanceOf(AuthException.class);
            // verify
            verifyNoInteractions(fingerprintFactory, handoffStore);
        }

        private CreateInstanceAdminSessionHandoffRequest request(long userAuthVersion) {
            return CreateInstanceAdminSessionHandoffRequest.builder()
                    .userId(USER_ID)
                    .instanceId(INSTANCE_ID)
                    .userAuthVersion(userAuthVersion)
                    .adminSessionVersion(9L)
                    .preAuthBinding(PreAuthTransactionBinding.fromHash(PRE_AUTH_BINDING))
                    .clientIp("192.0.2.10")
                    .userAgent("Admin browser")
                    .build();
        }

    }

}
