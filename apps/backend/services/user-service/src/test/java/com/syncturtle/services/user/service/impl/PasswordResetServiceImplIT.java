package com.syncturtle.services.user.service.impl;

import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.TOKEN_BYTES;
import static com.syncturtle.services.user.support.fixture.PasswordResetFixtures.TOKEN_TTL;
import static com.syncturtle.services.user.support.fixture.PasswordResetIntegrationFixtures.newOutstandingToken;
import static com.syncturtle.services.user.support.fixture.PasswordResetIntegrationFixtures.newUser;
import static com.syncturtle.services.user.support.fixture.PasswordResetIntegrationFixtures.uniqueRawToken;
import static com.syncturtle.services.user.support.fixture.PasswordResetIntegrationFixtures.uniqueTokenHash;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.syncturtle.common.core.security.token.SecureTokenGenerator;
import com.syncturtle.common.core.security.token.TokenHasher;
import com.syncturtle.services.user.configuration.property.AuthProperties;
import com.syncturtle.services.user.messaging.kafka.factory.AuthenticationEmailEventFactory;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.model.InstanceLite;
import com.syncturtle.services.user.model.PasswordResetToken;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.InstanceLiteRepository;
import com.syncturtle.services.user.repository.OutboxMessageRepository;
import com.syncturtle.services.user.repository.PasswordResetTokenRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.PasswordResetService;
import com.syncturtle.services.user.service.collaborator.authentication.password.PasswordResetUidCodec;
import com.syncturtle.services.user.service.collaborator.authentication.password.PasswordResetUrlBuilder;
import com.syncturtle.services.user.service.collaborator.outbox.EmailOutboxWriter;
import com.syncturtle.services.user.service.collaborator.outbox.OutboxMessageWriter;
import com.syncturtle.services.user.service.collaborator.outbox.UserOutboxWriter;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeConfigResolver;
import com.syncturtle.services.user.service.collaborator.runtime.UserAuthRuntimeSnapshot;
import com.syncturtle.services.user.service.collaborator.session.RefreshSessionTokenStore;
import com.syncturtle.services.user.support.clock.TestClocks;
import com.syncturtle.testing.annotation.IntegrationTest;
import com.syncturtle.testing.annotation.UsePostgresDb;

@IntegrationTest(classes = PasswordResetServiceImplIT.TestApplication.class)
@UsePostgresDb("user_service_password_reset_it")
@DisplayName("PasswordResetService integration")
class PasswordResetServiceImplIT {

    @Autowired
    PasswordResetService service;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    OutboxMessageRepository outboxMessageRepository;
    @Autowired
    PasswordResetUidCodec uidCodec;
    @Autowired
    Clock clock;

    @MockitoBean
    InstanceLiteRepository instanceRepository;
    @MockitoBean
    UserAuthRuntimeConfigResolver configFlagResolver;
    @MockitoBean
    PasswordResetUrlBuilder resetUrlBuilder;
    @MockitoBean
    SecureTokenGenerator tokenGenerator;
    @MockitoBean
    TokenHasher tokenHasher;
    @MockitoBean
    AuthProperties authProperties;
    @MockitoBean
    PasswordEncoder passwordEncoder;
    @MockitoBean
    RefreshSessionTokenStore refreshSessionTokenStore;
    @MockitoSpyBean
    EmailOutboxWriter emailOutboxWriter;
    @MockitoSpyBean
    UserOutboxWriter userOutboxWriter;

    @BeforeEach
    void setup() {
        InstanceLite instance = mock(InstanceLite.class);
        UserAuthRuntimeSnapshot configuration = mock(UserAuthRuntimeSnapshot.class);

        when(instance.isSetupDone()).thenReturn(true);
        when(configuration.isSmtpEnabled()).thenReturn(true);
        when(instanceRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(instance));
        when(configFlagResolver.getInstanceConfigurations()).thenReturn(configuration);
        when(authProperties.getPasswordResetTokenBytes()).thenReturn(TOKEN_BYTES);
        when(authProperties.getPasswordResetTokenTtl()).thenReturn(TOKEN_TTL);
    }

    @Nested
    @DisplayName("requestReset(String)")
    class RequestResetTests {

        @Test
        @DisplayName("invalidates outstanding tokens and persists a token and email outbox message")
        void invalidatesOutstandingTokensAndPersistsTokenAndOutboxMessage() {
            // arrange
            User user = persistUser();
            PasswordResetToken outstandingToken = persistOutstandingToken(user, uniqueTokenHash("outstanding"));
            String rawToken = uniqueRawToken("new-reset-token");
            String tokenhash = uniqueTokenHash("new-reset-token");
            String resetUrl = "https://app.syncturtle.test/accounts/reset-password/";

            stubIssuedResetToken(user, rawToken, tokenhash, resetUrl);
            // act
            service.requestReset(" " + user.getEmail().toUpperCase(Locale.ROOT) + " ");
            // assert
            PasswordResetToken persistedOutstandingToken = requiredToken(outstandingToken.getId());
            assertThat(persistedOutstandingToken.getInvalidatedAt()).isEqualTo(TestClocks.NOW);
        }

    }

    private User persistUser() {
        return userRepository.saveAndFlush(newUser(clock));
    }

    private PasswordResetToken persistOutstandingToken(User user, String tokenHash) {
        return passwordResetTokenRepository.saveAndFlush(newOutstandingToken(user, tokenHash));
    }

    private void stubIssuedResetToken(User user, String rawToken, String tokenHash, String resetUrl) {
        when(tokenGenerator.generateBase64Url(TOKEN_BYTES)).thenReturn(rawToken);
        when(tokenHasher.hash(rawToken)).thenReturn(tokenHash);
        when(resetUrlBuilder.build(eq(uidCodec.encode(user.getId())), eq(rawToken))).thenReturn(resetUrl);
    }

    private PasswordResetToken requiredToken(UUID tokenId) {
        return passwordResetTokenRepository.findById(tokenId).orElseThrow();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = User.class)
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    @Import({
            PasswordResetServiceImpl.class,
            PasswordResetUidCodec.class,
            AuthenticationEmailEventFactory.class,
            UserEventFactory.class,
            OutboxMessageWriter.class,
            EmailOutboxWriter.class,
            UserOutboxWriter.class
    })
    static class TestApplication {

        @Bean
        Clock clock() {
            return TestClocks.fixedUtc();
        }

        @Bean("testDateTimeProvider")
        DateTimeProvider testDateTimeProvider(Clock clock) {
            return () -> Optional.of(clock.instant());
        }

        @Bean
        AuditorAware<UUID> testAuditorAware() {
            return Optional::empty;
        }

    }

}
