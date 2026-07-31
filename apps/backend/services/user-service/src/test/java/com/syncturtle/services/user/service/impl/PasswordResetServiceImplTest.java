package com.syncturtle.services.user.service.impl;

import static com.syncturtle.services.user.support.assertion.AuthExceptionAssert.assertThatAuthExceptionThrownBy;
import static com.syncturtle.services.user.support.factory.PasswordResetMockFactory.configuredInstance;
import static com.syncturtle.services.user.support.factory.PasswordResetMockFactory.outstandingResetToken;
import static com.syncturtle.services.user.support.factory.PasswordResetMockFactory.runtimeConfig;
import static com.syncturtle.services.user.support.factory.PasswordResetMockFactory.user;
import static com.syncturtle.services.user.support.factory.PasswordResetMockFactory.validResetToken;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.BLANK_PASSWORD;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.EMAIL;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.EMAIL_WITH_WHITESPACE_AND_CASE;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.INVALID_EMAIL;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.NORMALIZED_EMAIL;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.OTHER_RESET_TOKEN_ID;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.PASSWORD_HASH;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.RAW_TOKEN;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.RESET_TOKEN_ID;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.RESET_URL;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.SHORT_PASSWORD;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.TOKEN_BYTES;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.TOKEN_HASH;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.TOKEN_TTL;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.UIDB64;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.USER_ID;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.VALID_PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.email.event.EmailToSendEvent;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.services.user.configuration.property.AuthProperties;
import com.syncturtle.services.user.messaging.kafka.factory.AuthenticationEmailEventFactory;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.model.InstanceLite;
import com.syncturtle.services.user.model.PasswordResetToken;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceLiteRepository;
import com.syncturtle.services.user.repository.PasswordResetTokenRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.PasswordResetService;
import com.syncturtle.services.user.service.collaborator.authentication.password.PasswordResetUidCodec;
import com.syncturtle.services.user.service.collaborator.authentication.password.PasswordResetUrlBuilder;
import com.syncturtle.services.user.service.collaborator.outbox.EmailOutboxWriter;
import com.syncturtle.services.user.service.collaborator.outbox.UserOutboxWriter;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeSnapshot;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionTokenStore;
import com.syncturtle.services.user.support.clock.TestClocks;
import com.syncturtle.services.user.support.fixture.PasswordResetFixtures;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("PasswordResetService")
class PasswordResetServiceImplTest {

    @Mock
    InstanceLiteRepository instanceRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    UserAuthRuntimeConfigResolver configFlagResolver;
    @Mock
    PasswordResetUidCodec uidCodec;
    @Mock
    PasswordResetUrlBuilder resetUrlBuilder;
    @Mock
    SecureTokenGenerator tokenGenerator;
    @Mock
    TokenHasher tokenHasher;
    @Mock
    AuthProperties authProperties;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    RefreshSessionTokenStore refreshSessionTokenStore;
    @Mock
    AuthenticationEmailEventFactory emailEventFactory;
    @Mock
    EmailOutboxWriter emailOutboxWriter;
    @Mock
    UserEventFactory userEventFactory;
    @Mock
    UserOutboxWriter userOutboxWriter;

    @Captor
    ArgumentCaptor<PasswordResetToken> savedResetTokenCaptor;

    private PasswordResetService service;

    @BeforeEach
    void setup() {
        Clock clock = TestClocks.fixedUtc();
        service = new PasswordResetServiceImpl(
                instanceRepository,
                userRepository,
                passwordResetTokenRepository,
                configFlagResolver,
                uidCodec,
                resetUrlBuilder,
                tokenGenerator,
                tokenHasher,
                authProperties,
                passwordEncoder,
                refreshSessionTokenStore,
                emailEventFactory,
                emailOutboxWriter,
                userEventFactory,
                userOutboxWriter,
                clock);
    }

    @Nested
    @DisplayName("requestReset(String)")
    class RequestResetTests {

        @Test
        @DisplayName("invalidates existing tokens and writes password reset email to outbox")
        void invalidatesExistingTokensAndWritesPasswordResetEmailToOutbox() {
            // arrange
            User existingUser = user(USER_ID);
            PasswordResetToken firstOutstandingToken = outstandingResetToken();
            PasswordResetToken secondOutstandingToken = outstandingResetToken();
            EmailToSendEvent emailEvent = mock(EmailToSendEvent.class);
            // conditions
            stubConfiguredPasswordReset();
            when(userRepository.findByEmailIgnoreCaseForUpdate(NORMALIZED_EMAIL)).thenReturn(Optional.of(existingUser));
            when(passwordResetTokenRepository.findOutstandingByUserIdForUpdate(USER_ID))
                    .thenReturn(List.of(firstOutstandingToken, secondOutstandingToken));
            when(authProperties.getPasswordResetTokenBytes()).thenReturn(TOKEN_BYTES);
            when(authProperties.getPasswordResetTokenTtl()).thenReturn(TOKEN_TTL);
            when(tokenGenerator.generateBase64Url(TOKEN_BYTES)).thenReturn(RAW_TOKEN);
            when(tokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
            when(uidCodec.encode(USER_ID)).thenReturn(UIDB64);
            when(resetUrlBuilder.build(UIDB64, RAW_TOKEN)).thenReturn(RESET_URL);
            when(emailEventFactory.passwordReset(NORMALIZED_EMAIL, RESET_URL, TOKEN_TTL)).thenReturn(emailEvent);
            // act
            service.requestReset(EMAIL_WITH_WHITESPACE_AND_CASE);
            // assert + verify
            verify(passwordResetTokenRepository).save(savedResetTokenCaptor.capture());

            PasswordResetToken savedResetToken = savedResetTokenCaptor.getValue();
            assertThat(savedResetToken).isNotNull();

            verify(firstOutstandingToken).invalidate(TestClocks.NOW);
            verify(secondOutstandingToken).invalidate(TestClocks.NOW);
            verify(emailOutboxWriter).saveEmailToSendEvent(emailEvent, USER_ID);

            InOrder order = inOrder(
                    instanceRepository,
                    configFlagResolver,
                    userRepository,
                    passwordResetTokenRepository,
                    firstOutstandingToken,
                    secondOutstandingToken,
                    tokenGenerator,
                    tokenHasher,
                    uidCodec,
                    resetUrlBuilder,
                    emailEventFactory,
                    emailOutboxWriter);

            order.verify(instanceRepository).findFirstByOrderByCreatedAtAsc();
            order.verify(configFlagResolver).getInstanceConfigurations();
            order.verify(userRepository).findByEmailIgnoreCaseForUpdate(NORMALIZED_EMAIL);
            order.verify(passwordResetTokenRepository).findOutstandingByUserIdForUpdate(USER_ID);
            order.verify(firstOutstandingToken).invalidate(TestClocks.NOW);
            order.verify(secondOutstandingToken).invalidate(TestClocks.NOW);
            order.verify(tokenGenerator).generateBase64Url(TOKEN_BYTES);
            order.verify(tokenHasher).hash(RAW_TOKEN);
            order.verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            order.verify(uidCodec).encode(USER_ID);
            order.verify(resetUrlBuilder).build(UIDB64, RAW_TOKEN);
            order.verify(emailEventFactory).passwordReset(NORMALIZED_EMAIL, RESET_URL, TOKEN_TTL);
            order.verify(emailOutboxWriter).saveEmailToSendEvent(emailEvent, USER_ID);

            verifyNoInteractions(passwordEncoder, refreshSessionTokenStore, userEventFactory, userOutboxWriter);
        }

        @Test
        @DisplayName("returns normally when use does not exist")
        void returnsNormallyWhenUserDoesNotExist() {
            // arrange
            stubConfiguredPasswordReset();
            // conditions
            when(userRepository.findByEmailIgnoreCaseForUpdate(EMAIL)).thenReturn(Optional.empty());
            // act + assert
            assertThatCode(() -> service.requestReset(EMAIL)).doesNotThrowAnyException();
            // verify
            verify(userRepository).findByEmailIgnoreCaseForUpdate(EMAIL);

            verifyNoInteractions(
                    passwordResetTokenRepository,
                    authProperties,
                    tokenGenerator,
                    tokenHasher,
                    uidCodec,
                    resetUrlBuilder,
                    emailEventFactory,
                    emailOutboxWriter);
        }

        @Test
        @DisplayName("throws INSTANCE_NOT_CONFIGURED when instance does not exist")
        void throwsInstanceNotConfiguredWhenInstanceDoesNotExist() {
            // arrange
            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
            // act + assert
            assertThatAuthExceptionThrownBy(() -> service.requestReset(EMAIL))
                    .hasErrorCode(AuthErrorCode.INSTANCE_NOT_CONFIGURED)
                    .hasNoPayload();
            // verify
            verify(instanceRepository).findFirstByOrderByCreatedAtAsc();

            verifyNoInteractions(
                    configFlagResolver,
                    userRepository,
                    passwordResetTokenRepository,
                    tokenGenerator,
                    tokenHasher,
                    emailOutboxWriter);
        }

        @Test
        @DisplayName("throws SMTP_NOT_CONFIGURED when SMTP is disabled")
        void throwsSmtpNotConfiguredWhenSmtpIsDisabled() {
            // arrange
            InstanceLite instance = configuredInstance();
            UserAuthRuntimeSnapshot configuration = runtimeConfig(false);
            // conditions
            when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
            when(configFlagResolver.getInstanceConfigurations()).thenReturn(configuration);
            // act + assert
            assertThatAuthExceptionThrownBy(() -> service.requestReset(EMAIL))
                    .hasErrorCode(AuthErrorCode.SMTP_NOT_CONFIGURED)
                    .hasNoPayload();
            // verify
            verify(configFlagResolver).getInstanceConfigurations();

            verifyNoInteractions(
                    userRepository,
                    passwordResetTokenRepository,
                    tokenGenerator,
                    tokenHasher,
                    emailOutboxWriter);
        }

        @Test
        @DisplayName("throws INVALID_EMAIL when email is malformed")
        void throwsInvalidEmailWhenEmailIsMalformed() {
            // arrange
            stubConfiguredPasswordReset();
            // act + assert
            assertThatAuthExceptionThrownBy(() -> service.requestReset(INVALID_EMAIL))
                    .hasErrorCode(AuthErrorCode.INVALID_EMAIL);
            // verify
            verifyNoInteractions(
                    userRepository,
                    passwordResetTokenRepository,
                    tokenGenerator,
                    tokenHasher,
                    emailOutboxWriter);
        }

    }

    @Nested
    @DisplayName("resetPassword(String, String, String)")
    class ResetPasswordTests {

        @Test
        @DisplayName("changes password, consumes token, revokes sessions, and writes event")
        void changesPasswordConsumesTokenRevokesSessionsAndWritesEvent() {
            // arrange
            User existingUser = user(USER_ID);
            PasswordResetToken resetToken = validResetToken(RESET_TOKEN_ID);
            PasswordResetToken otherResetToken = outstandingResetToken(OTHER_RESET_TOKEN_ID);
            UserEvent userEvent = mock(UserEvent.class);
            stubResetTokenLookup(existingUser, resetToken);
            // conditions
            when(passwordResetTokenRepository.findOutstandingByUserIdForUpdate(USER_ID))
                    .thenReturn(List.of(resetToken, otherResetToken));
            when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(PASSWORD_HASH);
            when(userEventFactory.updated(existingUser)).thenReturn(userEvent);
            // act
            service.resetPassword(UIDB64, RAW_TOKEN, VALID_PASSWORD);
            // verify
            verify(resetToken).consume(TestClocks.NOW);
            verify(resetToken, never()).invalidate(any(Instant.class));
            verify(otherResetToken).invalidate(TestClocks.NOW);
            verify(existingUser).markPasswordChanged(PASSWORD_HASH, false);
            verify(userRepository).flush();
            verify(refreshSessionTokenStore).revokeUserSessions(USER_ID.toString());
            verify(userEventFactory).updated(existingUser);
            verify(userOutboxWriter).saveUserEvent(userEvent);
            // verify order
            InOrder order = inOrder(
                    uidCodec,
                    userRepository,
                    tokenHasher,
                    passwordResetTokenRepository,
                    passwordEncoder,
                    resetToken,
                    otherResetToken,
                    existingUser,
                    refreshSessionTokenStore,
                    userEventFactory,
                    userOutboxWriter);
            order.verify(uidCodec).decode(UIDB64);
            order.verify(userRepository).findByIdForUpdate(USER_ID);
            order.verify(tokenHasher).hash(RAW_TOKEN);
            order.verify(passwordResetTokenRepository).findByUserIdAndTokenHashForUpdate(USER_ID, TOKEN_HASH);
            order.verify(passwordResetTokenRepository).findOutstandingByUserIdForUpdate(USER_ID);
            order.verify(passwordEncoder).encode(VALID_PASSWORD);
            order.verify(resetToken).consume(TestClocks.NOW);
            order.verify(otherResetToken).invalidate(TestClocks.NOW);
            order.verify(existingUser).markPasswordChanged(PASSWORD_HASH, false);
            order.verify(userRepository).flush();
            order.verify(refreshSessionTokenStore).revokeUserSessions(USER_ID.toString());
            order.verify(userEventFactory).updated(existingUser);
            order.verify(userOutboxWriter).saveUserEvent(userEvent);
        }

        @Test
        @DisplayName("throws INVALID_PASSWORD when password is blank")
        void throwsInvalidPasswordWhenPasswordIsBlank() {
            // act + assert
            assertThatAuthExceptionThrownBy(() -> service.resetPassword(UIDB64, RAW_TOKEN, BLANK_PASSWORD))
                    .hasErrorCode(AuthErrorCode.INVALID_PASSWORD);
            // verify
            verifyNoInteractions(
                    uidCodec,
                    userRepository,
                    passwordResetTokenRepository,
                    tokenHasher,
                    passwordEncoder,
                    refreshSessionTokenStore,
                    userEventFactory,
                    userOutboxWriter);
        }

        @Test
        @DisplayName("throws INVALID_PASSWORD when password is too short")
        void throwsInvalidPasswordWhenPasswordIsTooShort() {
            // act + assert
            assertThatAuthExceptionThrownBy(() -> service.resetPassword(UIDB64, RAW_TOKEN, SHORT_PASSWORD))
                    .hasErrorCode(AuthErrorCode.INVALID_PASSWORD);
            // verify
            verifyNoInteractions(
                    uidCodec,
                    userRepository,
                    passwordResetTokenRepository,
                    tokenHasher,
                    passwordEncoder);
        }

        @Test
        @DisplayName("accepts password at maximum allowed length")
        void acceptsPasswordAtMaximumAllowedLength() {
            // arrange
            User existingUser = user(USER_ID);
            PasswordResetToken resetToken = validResetToken(RESET_TOKEN_ID);
            String maximumLengthPassword = PasswordResetFixtures.maximumLengthPassword();
            UserEvent userEvent = mock(UserEvent.class);
            stubResetTokenLookup(existingUser, resetToken);
            // conditions
            when(passwordResetTokenRepository.findOutstandingByUserIdForUpdate(USER_ID))
                    .thenReturn(List.of(resetToken));
            when(passwordEncoder.encode(maximumLengthPassword)).thenReturn(PASSWORD_HASH);
            when(userEventFactory.updated(existingUser)).thenReturn(userEvent);
            // act + assert
            assertThatCode(() -> service.resetPassword(UIDB64, RAW_TOKEN, maximumLengthPassword))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws INVALID_PASSWORD_TOKEN when uid is blank")
        void throwsInvalidPasswordTokenWhenUidIsBlank() {
            // act + assert
            assertThatAuthExceptionThrownBy(() -> service.resetPassword(" ", RAW_TOKEN, VALID_PASSWORD))
                    .hasErrorCode(AuthErrorCode.INVALID_PASSWORD_TOKEN);
            // verify
            verifyNoInteractions(
                    uidCodec,
                    userRepository,
                    passwordResetTokenRepository,
                    tokenHasher,
                    passwordEncoder);
        }

    }

    private void stubConfiguredPasswordReset() {
        InstanceLite instance = configuredInstance();

        UserAuthRuntimeSnapshot configuration = runtimeConfig(true);

        when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
        when(configFlagResolver.getInstanceConfigurations()).thenReturn(configuration);
    }

    private void stubResetTokenLookup(User existingUser, PasswordResetToken resetToken) {
        when(uidCodec.decode(UIDB64)).thenReturn(Optional.of(USER_ID));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(existingUser));
        when(tokenHasher.hash(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(passwordResetTokenRepository.findByUserIdAndTokenHashForUpdate(USER_ID, TOKEN_HASH))
                .thenReturn(Optional.of(resetToken));
    }

}
