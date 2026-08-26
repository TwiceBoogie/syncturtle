package com.syncturtle.services.instance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScope;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.services.instance.messaging.kafka.factory.InstanceConfigurationEventFactory;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceConfiguration;
import com.syncturtle.services.instance.repository.InstanceConfigurationRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.InstanceConfigurationService;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationCrypto;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.collaborator.outbox.InstanceOutboxWriter;
import com.syncturtle.services.instance.service.param.RequestedKeyParam;
import com.syncturtle.services.instance.service.result.InstanceConfigResult;
import com.syncturtle.services.instance.support.clock.TestClocks;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;
import com.syncturtle.services.instance.type.InstanceConfigurationCategory;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("InstanceConfigurationService")
class InstanceConfigurationServiceImplTest {

    @Mock
    InstanceConfigurationRepository configurationRepository;
    @Mock
    InstanceRepository instanceRepository;
    @Mock
    InstanceConfigurationResolver configurationResolver;
    @Mock
    InstanceOutboxWriter outboxWriter;
    @Mock
    InstanceConfigurationCrypto crypto;

    private InstanceConfigurationService service;

    @BeforeEach
    void setup() {
        Clock clock = TestClocks.fixedUtc();
        InstanceConfigurationEventFactory eventFactory = new InstanceConfigurationEventFactory(clock);
        service = new InstanceConfigurationServiceImpl(configurationRepository, instanceRepository,
                configurationResolver, outboxWriter, eventFactory, crypto, clock);
    }

    @Nested
    @DisplayName("configurations()")
    class ConfigurationsTests {

        @Test
        @DisplayName("resolves public instance configuration keys and returns result")
        void resolvesPublicInstanceConfigurationKeysAndReturnsResult() {
            // arrange
            ArgumentCaptor<List<RequestedKeyParam>> requested = ArgumentCaptor.captor();
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            Map<InstanceConfigurationKey, String> values = new EnumMap<>(InstanceConfigurationKey.class);
            values.put(InstanceConfigurationKey.ENABLE_SIGNUP, "1");
            values.put(InstanceConfigurationKey.IS_GITHUB_ENABLED, "1");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(configurationResolver.resolveRequested(anyList())).thenReturn(values);
            // act
            InstanceConfigResult actual = service.configurations();
            // assert
            assertThat(actual.getValues()).isSameAs(values);
            assertThat(actual.getVersion()).isEqualTo(instance.getConfig().getVersion());
            // verify
            verify(configurationResolver).resolveRequested(requested.capture());

            assertThat(requested.getValue()).extracting(RequestedKeyParam::getKey)
                    .contains(
                            InstanceConfigurationKey.ENABLE_SIGNUP,
                            InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION,
                            InstanceConfigurationKey.IS_GOOGLE_ENABLED,
                            InstanceConfigurationKey.IS_GITHUB_ENABLED,
                            InstanceConfigurationKey.GITHUB_APP_NAME,
                            InstanceConfigurationKey.IS_GITLAB_ENABLED,
                            InstanceConfigurationKey.EMAIL_HOST,
                            InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN,
                            InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD,
                            InstanceConfigurationKey.POSTHOG_API_KEY,
                            InstanceConfigurationKey.POSTHOG_HOST,
                            InstanceConfigurationKey.IS_INTERCOM_ENABLED,
                            InstanceConfigurationKey.INTERCOM_APP_ID);
        }

    }

    @Nested
    @DisplayName("configurationsAll()")
    class ConfigurationsAllTests {

        @Test
        @DisplayName("loads all configuration row and decypts values for response")
        void loadsAllConfigurationRowAndDecryptsValuesForResponse() {
            // arrange
            InstanceConfiguration smtp = row(InstanceConfigurationKey.ENABLE_SMTP, "1");
            InstanceConfiguration password = InstanceConfiguration.create(InstanceConfigurationKey.EMAIL_HOST_PASSWORD,
                    "encrypted-secret", InstanceConfigurationCategory.SMTP.value(), true);
            // conditions
            when(configurationRepository.findAll()).thenReturn(List.of(smtp, password));
            when(crypto.decryptIfNeeded(same(smtp))).thenReturn("1");
            when(crypto.decryptIfNeeded(same(password))).thenReturn("plain-secret");
            // act
            List<InstanceConfigurationResponse> actual = service.configurationsAll();
            // assert
            assertThat(actual)
                    .extracting(InstanceConfigurationResponse::getKey,
                            InstanceConfigurationResponse::getValue)
                    .containsExactly(
                            tuple(InstanceConfigurationKey.ENABLE_SMTP, "1"),
                            tuple(InstanceConfigurationKey.EMAIL_HOST_PASSWORD, "plain-secret"));
            // verify
            verify(configurationRepository).findAll();
            verify(crypto).decryptIfNeeded(same(smtp));
            verify(crypto).decryptIfNeeded(same(password));
        }

    }

    @Nested
    @DisplayName("configurationsUpdate(Map)")
    class ConfigurationsUpdateTests {

        @Test
        @DisplayName("returns empty list for null request without hitting repositories")
        void returnsEmptyListForNullRequestWithoutHittingRepositories() {
            // arrange
            // conditions
            // act
            List<InstanceConfigurationResponse> actual = service.configurationsUpdate(null);
            // assert
            assertThat(actual).isEmpty();
            // verify
            verifyNoInteractions(configurationRepository, instanceRepository, configurationResolver, crypto,
                    outboxWriter);
        }

        @Test
        @DisplayName("returns empty list for empty request without hitting repositories")
        void returnsEmptyListForEmptyRequestWithoutHittingRepositories() {
            // arrange
            // conditions
            // act
            List<InstanceConfigurationResponse> actual = service.configurationsUpdate(Map.of());
            // assert
            assertThat(actual).isEmpty();
            // verify
            verifyNoInteractions(configurationRepository, instanceRepository, configurationResolver, crypto,
                    outboxWriter);
        }

        @Test
        @DisplayName("returns empty list when requested values do not change stored values")
        void returnsEmptyListWhenRequestedValuesDoNotChangeStoredValues() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            InstanceConfiguration signup = row(InstanceConfigurationKey.ENABLE_SIGNUP, "1");
            Map<InstanceConfigurationKey, String> request = Map.of(InstanceConfigurationKey.ENABLE_SIGNUP, "1");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(configurationRepository.findByKeyIn(request.keySet())).thenReturn(List.of(signup));
            when(crypto.decryptIfNeeded(signup)).thenReturn("1");
            // act
            List<InstanceConfigurationResponse> actual = service.configurationsUpdate(request);
            // assert
            assertThat(actual).isEmpty();
            // verify
            verify(instanceRepository, never()).saveAndFlush(any(Instance.class));
            verify(configurationRepository, never()).saveAll(anyCollection());
            verifyNoInteractions(outboxWriter);
        }

        @Test
        @DisplayName("returns only changed persisted rows and increments once")
        void returnsOnlyChangedRowsAndIncrementsOnce() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            long originalVersion = instance.getConfig().getVersion();
            InstanceConfiguration signup = row(InstanceConfigurationKey.ENABLE_SIGNUP, "1");
            Map<InstanceConfigurationKey, String> request = Map.of(InstanceConfigurationKey.ENABLE_SIGNUP, "0");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(configurationRepository.findByKeyIn(request.keySet())).thenReturn(List.of(signup));
            when(crypto.decryptIfNeeded(same(signup))).thenReturn("1", "0");
            when(crypto.encryptIfNeeded(false, "0")).thenReturn("0");
            // act
            List<InstanceConfigurationResponse> actual = service.configurationsUpdate(request);
            // assert
            assertThat(actual)
                    .extracting(InstanceConfigurationResponse::getKey)
                    .containsExactly(InstanceConfigurationKey.ENABLE_SIGNUP);
            assertThat(instance.getConfig().getVersion()).isEqualTo(originalVersion + 1);
            // verify
            verify(outboxWriter).saveInstanceConfigurationEvent(any());
        }

    }

    @Nested
    @DisplayName("disableEmail()")
    class DisableEmailTests {

        @Test
        @DisplayName("does nothing when email keys are already disabled or blank")
        void noOpWhenAlreadyDisabled() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            InstanceConfiguration enableSmtp = row(InstanceConfigurationKey.ENABLE_SMTP, "0");
            InstanceConfiguration emailHost = InstanceConfiguration.create(
                    InstanceConfigurationKey.EMAIL_HOST, "",
                    InstanceConfigurationCategory.SMTP.value(), false);
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(configurationRepository.findByKeyIn(anySet())).thenReturn(List.of(enableSmtp, emailHost));
            when(crypto.decryptIfNeeded(same(enableSmtp))).thenReturn("0");
            when(crypto.decryptIfNeeded(same(emailHost))).thenReturn("");
            // act
            service.disableEmail();
            // verify
            verify(instanceRepository, never()).saveAndFlush(any(Instance.class));
            verify(configurationRepository, never()).saveAll(anyCollection());
            verifyNoInteractions(outboxWriter);
        }

        @Test
        @DisplayName("clears email rows and publishes AUTH and EMAIL at one revision")
        void clearsEmailAtOneRevision() {
            // arrange
            ArgumentCaptor<InstanceConfigurationEvent> events = ArgumentCaptor.captor();
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            long before = instance.getConfig().getVersion();
            InstanceConfiguration enableSmtp = row(InstanceConfigurationKey.ENABLE_SMTP, "1");
            InstanceConfiguration emailHost = InstanceConfiguration.create(
                    InstanceConfigurationKey.EMAIL_HOST,
                    "smtp.example.com",
                    InstanceConfigurationCategory.SMTP.value(),
                    false);
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(configurationRepository.findByKeyIn(anySet()))
                    .thenReturn(List.of(enableSmtp, emailHost));
            when(crypto.decryptIfNeeded(same(enableSmtp))).thenReturn("1");
            when(crypto.decryptIfNeeded(same(emailHost))).thenReturn("smtp.example.com");
            when(crypto.encryptIfNeeded(false, "0")).thenReturn("0");
            when(crypto.encryptIfNeeded(false, "")).thenReturn("");
            // act
            service.disableEmail();
            // assert
            assertThat(enableSmtp.getValue()).isEqualTo("0");
            assertThat(emailHost.getValue()).isEqualTo("");
            assertThat(instance.getConfig().getVersion()).isEqualTo(before + 1);
            // verify
            verify(outboxWriter, times(2)).saveInstanceConfigurationEvent(events.capture());
            assertThat(events.getAllValues()).extracting(InstanceConfigurationEvent::getScope)
                    .containsExactlyInAnyOrder(InstanceConfigurationScope.AUTH, InstanceConfigurationScope.EMAIL);
            assertThat(events.getAllValues()).flatExtracting(InstanceConfigurationEvent::getChangedKeys)
                    .containsExactlyInAnyOrder("ENABLE_SMTP", "ENABLE_SMTP", "EMAIL_HOST");
            assertThat(events.getAllValues()).extracting(InstanceConfigurationEvent::getConfigurationVersion)
                    .containsOnly(before + 1);
            assertThat(events.getAllValues()).extracting(InstanceConfigurationEvent::getCorrelationId)
                    .containsOnly(events.getValue().getCorrelationId());
        }

    }

    private static InstanceConfiguration row(InstanceConfigurationKey key, String value) {
        return InstanceConfiguration.create(key, value, InstanceConfigurationCategory.AUTHENTICATION.value(), false);
    }

}
