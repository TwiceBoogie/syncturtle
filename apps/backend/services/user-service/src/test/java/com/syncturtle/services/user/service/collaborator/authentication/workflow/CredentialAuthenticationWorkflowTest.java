package com.syncturtle.services.user.service.collaborator.authentication.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.core.actor.PrincipalType;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.model.Profile;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.ProfileRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.collaborator.outbox.UserOutboxWriter;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeSnapshot;
import com.syncturtle.services.user.service.param.CredentialCompletionParam;
import com.syncturtle.services.user.service.param.CredentialUserDataParam;
import com.syncturtle.services.user.support.clock.TestClocks;
import com.syncturtle.services.user.type.CredentialProviderType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("CredentialAuthenticationWorkflow")
class CredentialAuthenticationWorkflowTest {

    private static final String EMAIL = "normal.user@example.com";
    private static final String RAW_PASSWORD = "correct-horse-battery-staple";
    private static final String PASSWORD_HASH = "$2a$10$encoded.password.hash.with.sufficient.length";
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final String USER_AGENT = "credential-workflow-test";

    @Mock
    UserRepository userRepository;
    @Mock
    ProfileRepository profileRepository;
    @Mock
    UserAuthRuntimeConfigResolver configFlagResolver;
    @Mock
    PostUserAuthenticationWorkflow postUserAuthenticationWorkflow;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    UserEventFactory userEventFactory;
    @Mock
    UserOutboxWriter outboxWriter;

    private CredentialAuthenticationWorkflow workflow;

    @BeforeEach
    void setup() {
        Clock clock = TestClocks.fixedUtc();
        workflow = new CredentialAuthenticationWorkflow(
                userRepository,
                profileRepository,
                configFlagResolver,
                postUserAuthenticationWorkflow,
                passwordEncoder,
                userEventFactory,
                outboxWriter,
                clock);
    }

    @Nested
    @DisplayName("completeLoginOrSignup(CredentialCompletionParam)")
    class CompleteLoginOrSignupTests {

        @Test
        @DisplayName("creates a human user with profile and outbox event")
        void createsHumanUserWithProfileAndOutboxEvent() {
            // arrange
            CredentialCompletionParam param = credentialCompletionParam();
            UserAuthRuntimeSnapshot configuration = UserAuthRuntimeSnapshot.builder()
                    .signupEnabled(true)
                    .build();
            UserEvent createdEvent = mock(UserEvent.class);

            // conditions
            when(userRepository.findByEmailIgnoreCase(EMAIL, User.class)).thenReturn(Optional.empty());
            when(configFlagResolver.getInstanceConfigurations()).thenReturn(configuration);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
            when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userEventFactory.created(any(User.class))).thenReturn(createdEvent);

            // act
            User actual = workflow.completeLoginOrSignup(param);

            // assert
            ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).saveAndFlush(savedUserCaptor.capture());
            User savedUser = savedUserCaptor.getValue();

            assertThat(actual).isSameAs(savedUser);
            assertThat(savedUser.getPrincipalType()).isEqualTo(PrincipalType.HUMAN);
            assertThat(savedUser.getEmail()).isEqualTo(EMAIL);

            ArgumentCaptor<Profile> savedProfileCaptor = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepository).save(savedProfileCaptor.capture());
            assertThat(savedProfileCaptor.getValue().getUser()).isSameAs(savedUser);

            // verify
            verify(userEventFactory).created(savedUser);
            verify(outboxWriter).saveUserEvent(createdEvent);
            verify(postUserAuthenticationWorkflow)
                    .afterAuthentication(savedUser, true, CredentialProviderType.EMAIL_PASSWORD);
        }

        @Test
        @DisplayName("preserves the existing user authentication path")
        void preservesExistingUserAuthenticationPath() {
            // arrange
            CredentialCompletionParam param = credentialCompletionParam();
            User existingUser = mock(User.class);
            UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            // conditions
            when(userRepository.findByEmailIgnoreCase(EMAIL, User.class)).thenReturn(Optional.of(existingUser));
            when(userRepository.saveAndFlush(existingUser)).thenReturn(existingUser);
            when(existingUser.getId()).thenReturn(userId);
            when(profileRepository.existsByUserId(userId)).thenReturn(true);

            // act
            User actual = workflow.completeLoginOrSignup(param);

            // assert
            assertThat(actual).isSameAs(existingUser);

            // verify
            verify(existingUser).recordSuccessfullLogin(
                    CredentialProviderType.EMAIL_PASSWORD.value(),
                    IP_ADDRESS,
                    USER_AGENT,
                    TestClocks.fixedUtc());
            verify(postUserAuthenticationWorkflow)
                    .afterAuthentication(existingUser, false, CredentialProviderType.EMAIL_PASSWORD);
            verify(profileRepository, never()).save(any(Profile.class));
            verifyNoInteractions(configFlagResolver, passwordEncoder, userEventFactory, outboxWriter);
        }
    }

    private CredentialCompletionParam credentialCompletionParam() {
        CredentialUserDataParam userData = CredentialUserDataParam.builder()
                .email(EMAIL)
                .rawPassword(RAW_PASSWORD)
                .build();

        return CredentialCompletionParam.builder()
                .provider(CredentialProviderType.EMAIL_PASSWORD)
                .userData(userData)
                .ipAddress(IP_ADDRESS)
                .userAgent(USER_AGENT)
                .build();
    }
}
