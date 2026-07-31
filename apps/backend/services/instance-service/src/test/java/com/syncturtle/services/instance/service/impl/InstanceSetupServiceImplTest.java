package com.syncturtle.services.instance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

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

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.services.instance.bootstrap.InstanceRegistrar;
import com.syncturtle.services.instance.messaging.kafka.factory.InstanceConfigurationEventFactory;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceConfiguration;
import com.syncturtle.services.instance.repository.InstanceConfigurationRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.InstanceSetupService;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationCrypto;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationPolicy;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationPropertySource;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.collaborator.outbox.InstanceOutboxWriter;
import com.syncturtle.services.instance.support.clock.TestClocks;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;
import com.syncturtle.services.instance.type.InstanceConfigurationCategory;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("InstanceSetupService")
class InstanceSetupServiceImplTest {

    private static final EnumSet<InstanceConfigurationKey> DERIVED_KEYS = EnumSet.of(
            InstanceConfigurationKey.IS_GOOGLE_ENABLED,
            InstanceConfigurationKey.IS_GITHUB_ENABLED,
            InstanceConfigurationKey.IS_GITLAB_ENABLED,
            InstanceConfigurationKey.IS_INTERCOM_ENABLED);

    @Mock
    InstanceRegistrar registrar;
    @Mock
    InstanceRepository instanceRepository;
    @Mock
    InstanceConfigurationRepository configurationRepository;
    @Mock
    InstanceConfigurationPolicy policy;
    @Mock
    InstanceConfigurationPropertySource valueSource;
    @Mock
    InstanceConfigurationResolver resolver;
    @Mock
    InstanceConfigurationCrypto crypto;
    @Mock
    InstanceOutboxWriter outboxWriter;

    @Captor
    ArgumentCaptor<Iterable<InstanceConfiguration>> configurationRowCaptor;
    @Captor
    ArgumentCaptor<InstanceConfigurationEvent> configurationEventCaptor;

    private InstanceSetupService service;

    @BeforeEach
    void setup() {
        Clock clock = TestClocks.fixedUtc();
        InstanceConfigurationEventFactory eventFactory = new InstanceConfigurationEventFactory(clock);
        service = new InstanceSetupServiceImpl(registrar, instanceRepository, configurationRepository, policy,
                valueSource, resolver, crypto, eventFactory, outboxWriter, clock);
    }

    @Nested
    @DisplayName("setup(String)")
    class SetupTests {

        @Test
        @DisplayName("validates mandatory secrets before registering the instance")
        void validatesMandatorySecretsBeforeRegisteringInstance() {
            // arrange
            IllegalStateException failure = new IllegalStateException("mandatory secrets are missing");
            doThrow(failure).when(resolver).ensureMandatorySecretsPresentOrThrow();
            // act and assert
            assertThatThrownBy(() -> service.setup("machine-1")).isSameAs(failure);
            // verify
            verify(registrar, never()).run(any());
            verify(instanceRepository, never()).findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class);
            verify(configurationRepository, never()).findByKeyIn(anyCollection());
            verify(configurationRepository, never()).saveAll(any());
            verify(outboxWriter, never()).saveInstanceConfigurationEvent(any());
        }

        @Test
        @DisplayName("fails when registration does not create an instance")
        void failsWhenRegistrationDoesNotCreateInstance() {
            // arrange
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.empty());
            // act and assert
            assertThatThrownBy(() -> service.setup("machine-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Instance registration did not create an instance");
            // verify
            verify(registrar).run("machine-1");
            verify(configurationRepository, never()).findByKeyIn(anyCollection());
            verify(configurationRepository, never()).saveAll(any());
            verify(instanceRepository, never()).saveAndFlush(any());
            verify(outboxWriter, never()).saveInstanceConfigurationEvent(any());
        }

        @Test
        @DisplayName("is idempotent when effective configuration is unchanged")
        void isIdempotentWhenEffectiveConfigurationIsUnchanged() {
            // arrange
            Instance instance = stubInstance();
            long versionBeforeSetup = instance.getConfig().getVersion();
            stubConfigurationKeys(EnumSet.noneOf(InstanceConfigurationKey.class));
            List<InstanceConfiguration> existingRows = disabledDerivedRows();
            // conditions
            when(configurationRepository.findByKeyIn(anyCollection())).thenReturn(existingRows);
            when(resolver.resolveValue(any(InstanceConfigurationKey.class))).thenReturn("");
            // act
            service.setup("machine-1");
            // assert
            assertThat(instance.getConfig().getVersion()).isEqualTo(versionBeforeSetup);
            // verify
            verify(registrar).run("machine-1");
            verify(configurationRepository).saveAll(any());
            verify(configurationRepository).flush();
            verify(instanceRepository, never()).saveAndFlush(instance);
            verify(outboxWriter, never()).saveInstanceConfigurationEvent(any());
        }

        @Test
        @DisplayName("creates missing derived rows without revising unchanged effective configuration")
        void createsMissingDerivedRowsWithoutRevisingUnchangedEffectiveConfiguration() {
            // arrange
            Instance instance = stubInstance();
            long versionBeforeSetup = instance.getConfig().getVersion();
            stubConfigurationKeys(EnumSet.noneOf(InstanceConfigurationKey.class));
            // conditions
            when(configurationRepository.findByKeyIn(anyCollection())).thenReturn(List.of());
            when(policy.categoryFor(any(InstanceConfigurationKey.class)))
                    .thenReturn(InstanceConfigurationCategory.AUTHENTICATION.value());
            when(resolver.resolveValue(any(InstanceConfigurationKey.class)))
                    .thenAnswer(invocation -> {
                        InstanceConfigurationKey key = invocation.getArgument(0);
                        return disabledEffectiveValue(key);
                    });
            // act
            service.setup("machine-1");
            // verify
            verify(configurationRepository).saveAll(configurationRowCaptor.capture());
            verify(configurationRepository).flush();
            List<InstanceConfiguration> savedRows = copyRows(configurationRowCaptor.getValue());
            // assert
            assertThat(savedRows).hasSize(DERIVED_KEYS.size());

            for (InstanceConfigurationKey key : DERIVED_KEYS) {
                InstanceConfiguration row = requireConfiguration(savedRows, key);

                assertThat(row.getKey()).isEqualTo(key);
                assertThat(row.getValue()).isEqualTo("0");
                assertThat(row.getCategory()).isEqualTo(InstanceConfigurationCategory.AUTHENTICATION.value());
                assertThat(row.isEncrypted()).isFalse();
            }

            assertThat(instance.getConfig().getVersion()).isEqualTo(versionBeforeSetup);
            verify(instanceRepository, never()).saveAndFlush(instance);
            verify(outboxWriter, never()).saveInstanceConfigurationEvent(any());
        }

        @Test
        @DisplayName("inserts a missing managed row without revising equal effective configuration")
        void insertsMissingManagedRowWithoutRevisingEqualEffectiveConfiguration() {
            // arrange
            Instance instance = stubInstance();
            long versionBeforeSetup = instance.getConfig().getVersion();
            InstanceConfigurationKey managedKey = InstanceConfigurationKey.EMAIL_PORT;
            stubConfigurationKeys(EnumSet.of(managedKey));
            List<InstanceConfiguration> existingRows = disabledDerivedRows();
            // conditions
            when(configurationRepository.findByKeyIn(anyCollection())).thenReturn(existingRows);
            when(valueSource.getRaw(managedKey)).thenReturn(" ");
            when(policy.defaultFor(managedKey)).thenReturn("587");
            when(policy.isEncrypted(managedKey)).thenReturn(false);
            when(policy.categoryFor(managedKey)).thenReturn(InstanceConfigurationCategory.SMTP.value());
            when(crypto.encryptIfNeeded(false, "587")).thenReturn("587");
            when(resolver.resolveValue(any(InstanceConfigurationKey.class)))
                    .thenAnswer(invocation -> {
                        InstanceConfigurationKey key = invocation.getArgument(0);
                        return effectiveValueForManagedPort(key);
                    });
            // act
            service.setup(null);
            // verify
            verify(configurationRepository).saveAll(configurationRowCaptor.capture());
            verify(configurationRepository).flush();
            InstanceConfiguration insertedRow = requireConfiguration(configurationRowCaptor.getValue(), managedKey);
            // assert
            assertThat(insertedRow.getKey()).isEqualTo(managedKey);
            assertThat(insertedRow.getValue()).isEqualTo("587");
            assertThat(insertedRow.getCategory()).isEqualTo(InstanceConfigurationCategory.SMTP.value());
            assertThat(insertedRow.isEncrypted()).isFalse();
            assertThat(instance.getConfig().getVersion()).isEqualTo(versionBeforeSetup);
            // verify
            verify(registrar).run(null);
            verify(valueSource).getRaw(managedKey);
            verify(policy).defaultFor(managedKey);
            verify(crypto).encryptIfNeeded(false, "587");
            verify(instanceRepository, never()).saveAndFlush(instance);
            verify(outboxWriter, never()).saveInstanceConfigurationEvent(any());
        }

        @Test
        @DisplayName("revises once and publishes when an existing derived value materially changes")
        void revisesOnceAndPublishesForMaterialDerivedChange() {
            // arrange
            Instance instance = stubInstance();
            long versionBeforeSetup = instance.getConfig().getVersion();
            stubConfigurationKeys(EnumSet.noneOf(InstanceConfigurationKey.class));
            List<InstanceConfiguration> existingRows = disabledDerivedRows();
            AtomicBoolean configurationFlushed = new AtomicBoolean(false);
            // conditions
            when(configurationRepository.findByKeyIn(anyCollection())).thenReturn(existingRows);
            doAnswer(invocation -> {
                configurationFlushed.set(true);
                return null;
            }).when(configurationRepository).flush();
            when(resolver.resolveValue(any(InstanceConfigurationKey.class)))
                    .thenAnswer(invocation -> {
                        InstanceConfigurationKey key = invocation.getArgument(0);
                        return materialChangeValue(key, configurationFlushed.get());
                    });
            // act
            service.setup("machine-1");
            // assert
            InstanceConfiguration googleEnabledRow = requireConfiguration(existingRows,
                    InstanceConfigurationKey.IS_GOOGLE_ENABLED);
            assertThat(googleEnabledRow.getValue()).isEqualTo("1");
            assertThat(instance.getConfig().getVersion()).isEqualTo(versionBeforeSetup + 1);
            // verify
            verify(configurationRepository).flush();
            verify(instanceRepository).saveAndFlush(instance);
            verify(outboxWriter).saveInstanceConfigurationEvent(configurationEventCaptor.capture());
            InstanceConfigurationEvent event = configurationEventCaptor.getValue();
            assertThat(event.getChangedKeys()).containsExactly("IS_GOOGLE_ENABLED");
            assertThat(event.getConfigurationVersion()).isEqualTo(versionBeforeSetup + 1);
        }
    }

    private Instance stubInstance() {
        Instance instance = InstanceFixtures.persistedInstance("SyncTurtle");

        when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                .thenReturn(Optional.of(instance));
        return instance;
    }

    private void stubConfigurationKeys(EnumSet<InstanceConfigurationKey> managedKeys) {
        EnumSet<InstanceConfigurationKey> managedKeysCopy = EnumSet.noneOf(InstanceConfigurationKey.class);
        managedKeysCopy.addAll(managedKeys);
        EnumSet<InstanceConfigurationKey> derivedKeysCopy = EnumSet.noneOf(InstanceConfigurationKey.class);
        derivedKeysCopy.addAll(DERIVED_KEYS);
        when(policy.managedKeys()).thenReturn(managedKeysCopy);
        when(policy.derivedKeys()).thenReturn(derivedKeysCopy);
    }

    private static String disabledEffectiveValue(InstanceConfigurationKey key) {
        if (DERIVED_KEYS.contains(key)) {
            return "0";
        }

        return "";
    }

    private static String effectiveValueForManagedPort(InstanceConfigurationKey key) {
        if (key == InstanceConfigurationKey.EMAIL_PORT) {
            return "587";
        }

        if (DERIVED_KEYS.contains(key)) {
            return "0";
        }

        return "";
    }

    private static String materialChangeValue(InstanceConfigurationKey key, boolean configurationFlushed) {
        if (key == InstanceConfigurationKey.GOOGLE_CLIENT_ID || key == InstanceConfigurationKey.GOOGLE_CLIENT_SECRET) {
            return "configured";
        }

        if (key == InstanceConfigurationKey.IS_GOOGLE_ENABLED) {
            if (configurationFlushed) {
                return "1";
            }

            return "0";
        }

        if (DERIVED_KEYS.contains(key)) {
            return "0";
        }

        return "";
    }

    private static List<InstanceConfiguration> disabledDerivedRows() {
        List<InstanceConfiguration> rows = new ArrayList<>(DERIVED_KEYS.size());

        for (InstanceConfigurationKey key : DERIVED_KEYS) {
            InstanceConfiguration row = InstanceConfiguration.create(key, "0",
                    InstanceConfigurationCategory.AUTHENTICATION.value(), false);
            rows.add(row);
        }

        return rows;
    }

    private static List<InstanceConfiguration> copyRows(Iterable<InstanceConfiguration> rows) {
        List<InstanceConfiguration> copy = new ArrayList<>();
        for (InstanceConfiguration row : rows) {
            copy.add(row);
        }

        return copy;
    }

    private static InstanceConfiguration requireConfiguration(Iterable<InstanceConfiguration> rows,
            InstanceConfigurationKey requiredKey) {
        InstanceConfiguration configuration = findConfiguration(rows, requiredKey);
        assertThat(configuration).as("configuration row for %s", requiredKey).isNotNull();

        return configuration;
    }

    private static InstanceConfiguration findConfiguration(Iterable<InstanceConfiguration> rows,
            InstanceConfigurationKey requiredKey) {
        for (InstanceConfiguration row : rows) {
            if (row.getKey() == requiredKey) {
                return row;
            }
        }

        return null;
    }
}