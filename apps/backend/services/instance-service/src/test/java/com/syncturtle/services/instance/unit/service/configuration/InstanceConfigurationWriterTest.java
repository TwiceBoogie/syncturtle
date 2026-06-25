package com.syncturtle.services.instance.unit.service.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.syncturtle.services.instance.model.InstanceConfiguration;
import com.syncturtle.services.instance.repository.InstanceConfigurationRepository;
import com.syncturtle.services.instance.service.configuration.InstanceConfigurationCrypto;
import com.syncturtle.services.instance.service.configuration.InstanceConfigurationPolicy;
import com.syncturtle.services.instance.service.configuration.InstanceConfigurationPropertySource;
import com.syncturtle.services.instance.service.configuration.InstanceConfigurationWriter;
import com.syncturtle.services.instance.service.configuration.param.DerivedFlagParam;
import com.syncturtle.services.instance.type.InstanceConfigurationCategory;

@DisplayName("InstanceConfigurationWriter")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class InstanceConfigurationWriterTest {

    @Mock
    InstanceConfigurationRepository repository;
    @Mock
    InstanceConfigurationPolicy policy;
    @Mock
    InstanceConfigurationCrypto crypto;
    @Mock
    InstanceConfigurationPropertySource valueSource;
    @Captor
    ArgumentCaptor<List<InstanceConfiguration>> rowsCaptor;
    @Captor
    ArgumentCaptor<InstanceConfiguration> rowCaptor;

    InstanceConfigurationWriter writer;

    @BeforeEach
    void setup() {
        writer = new InstanceConfigurationWriter(repository, policy, crypto, valueSource);
    }

    @Nested
    @DisplayName("seedManagedKeysIfMissing()")
    class SeedManagedKeysIfMissingTests {

        @Test
        @DisplayName("returns zero when all managed keys already exist")
        void returnsZeroWhenAllManagedKeysAlreadyExist() {
            // arrange
            Set<InstanceConfigurationKey> managedKeys = EnumSet.of(
                    InstanceConfigurationKey.ENABLE_SIGNUP,
                    InstanceConfigurationKey.EMAIL_PORT);
            InstanceConfiguration signup = InstanceConfiguration.create(InstanceConfigurationKey.ENABLE_SIGNUP, "1",
                    InstanceConfigurationCategory.AUTHENTICATION.value(), false);
            InstanceConfiguration emailPort = InstanceConfiguration.create(InstanceConfigurationKey.EMAIL_PORT, "587",
                    InstanceConfigurationCategory.SMTP.value(), false);
            // conditions
            when(policy.managedKeys()).thenReturn(managedKeys);
            when(repository.findByKeyIn(managedKeys)).thenReturn(List.of(signup, emailPort));
            // act
            int actual = writer.seedManagedKeysIfMissing();
            // assert
            assertThat(actual).isZero();
            // verify
            verify(repository, never()).saveAll(anyCollection());
            verify(repository, never()).flush();
        }

        @Test
        @DisplayName("seeds only missing managed keys")
        void seedsOnlyMissingManagedKeys() {
            // arrange
            Set<InstanceConfigurationKey> managedKeys = EnumSet.of(
                    InstanceConfigurationKey.ENABLE_SIGNUP,
                    InstanceConfigurationKey.EMAIL_PORT);
            InstanceConfiguration existingSignup = InstanceConfiguration.create(InstanceConfigurationKey.ENABLE_SIGNUP,
                    "1",
                    InstanceConfigurationCategory.AUTHENTICATION.value(), false);
            // conditions
            when(policy.managedKeys()).thenReturn(managedKeys);
            when(repository.findByKeyIn(managedKeys)).thenReturn(List.of(existingSignup));

            when(valueSource.getRaw(InstanceConfigurationKey.EMAIL_PORT)).thenReturn(null);
            when(policy.defaultFor(InstanceConfigurationKey.EMAIL_PORT)).thenReturn("587");
            when(policy.isEncrypted(InstanceConfigurationKey.EMAIL_PORT)).thenReturn(false);
            when(crypto.encryptIfNeeded(false, "587")).thenReturn("587");
            when(policy.categoryFor(InstanceConfigurationKey.EMAIL_PORT))
                    .thenReturn(InstanceConfigurationCategory.SMTP.value());
            // act
            int actual = writer.seedManagedKeysIfMissing();
            // assert
            assertThat(actual).isEqualTo(1);
            // verify
            verify(policy).requireManagedKey(InstanceConfigurationKey.EMAIL_PORT);
            verify(repository).saveAll(rowsCaptor.capture());
            verify(repository).flush();

            assertThat(rowsCaptor.getValue()).hasSize(1);

            InstanceConfiguration inserted = rowsCaptor.getValue().get(0);

            assertThat(inserted.getKey()).isEqualTo(InstanceConfigurationKey.EMAIL_PORT);
            assertThat(inserted.getValue()).isEqualTo("587");
            assertThat(inserted.isEncrypted()).isFalse();
        }

    }

    @Nested
    @DisplayName("ensureDerivedFlagIfMIssing(DerivedFlagParam)")
    class EnsureDerivedFlagIfMissingTests {

        @Test
        @DisplayName("rejects null param")
        void rejectsNullParam() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> writer.ensureDerivedFlagIfMissing(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("derived flag param is required");
            // verify
            verifyNoInteractions(repository, policy, crypto, valueSource);
        }

        @Test
        @DisplayName("returns false when derived flag already exists")
        void returnsFalseWhenDerivedFlagAlreadyExists() {
            // arrange
            DerivedFlagParam param = DerivedFlagParam.builder()
                    .key(InstanceConfigurationKey.IS_GOOGLE_ENABLED)
                    .enabled(true)
                    .build();
            // conditions
            when(repository.existsByKey(InstanceConfigurationKey.IS_GOOGLE_ENABLED)).thenReturn(true);
            // act
            boolean actual = writer.ensureDerivedFlagIfMissing(param);
            // assert
            assertThat(actual).isFalse();
            // verify
            verify(policy).requireDerivedKey(InstanceConfigurationKey.IS_GOOGLE_ENABLED);
            verify(repository, never()).saveAndFlush(any(InstanceConfiguration.class));
        }

        @Test
        @DisplayName("creates enabled derived flag when missing")
        void createsEnabledDerivedFlagWhenMissing() {
            // arrange
            DerivedFlagParam param = DerivedFlagParam.builder()
                    .key(InstanceConfigurationKey.IS_GOOGLE_ENABLED)
                    .enabled(true)
                    .build();
            // conditions
            when(repository.existsByKey(InstanceConfigurationKey.IS_GOOGLE_ENABLED)).thenReturn(false);
            when(policy.categoryFor(InstanceConfigurationKey.IS_GOOGLE_ENABLED))
                    .thenReturn(InstanceConfigurationCategory.AUTHENTICATION.value());
            // act
            boolean actual = writer.ensureDerivedFlagIfMissing(param);
            // assert
            assertThat(actual).isTrue();
            // verify
            verify(policy).requireDerivedKey(InstanceConfigurationKey.IS_GOOGLE_ENABLED);
            verify(repository).saveAndFlush(rowCaptor.capture());

            InstanceConfiguration row = rowCaptor.getValue();

            assertThat(row.getKey()).isEqualTo(InstanceConfigurationKey.IS_GOOGLE_ENABLED);
            assertThat(row.getValue()).isEqualTo("1");
            assertThat(row.isEncrypted()).isFalse();
        }

        @Test
        @DisplayName("creates disabled derived flag when missing")
        void createsDisabledDerivedFlagWhenMissing() {
            // arrange
            DerivedFlagParam param = DerivedFlagParam.builder()
                    .key(InstanceConfigurationKey.IS_GITHUB_ENABLED)
                    .enabled(false)
                    .build();
            // conditions
            when(repository.existsByKey(InstanceConfigurationKey.IS_GITHUB_ENABLED)).thenReturn(false);
            when(policy.categoryFor(InstanceConfigurationKey.IS_GITHUB_ENABLED))
                    .thenReturn(InstanceConfigurationCategory.AUTHENTICATION.value());
            // act
            boolean actual = writer.ensureDerivedFlagIfMissing(param);
            // assert
            assertThat(actual).isTrue();
            // verify
            verify(repository).saveAndFlush(rowCaptor.capture());

            assertThat(rowCaptor.getValue().getValue()).isEqualTo("0");
        }

    }

    @Nested
    @DisplayName("replacePlainValues(Map<InstanceConfigurationKey, String>)")
    class ReplacePlainValuesTests {

        @Test
        @DisplayName("returns empty set for null input without hitting repositories")
        void returnsEmptySetForNullInputWithoutHittingRepository() {
            // arrange
            // conditions
            // act
            Set<InstanceConfigurationKey> actual = writer.replacePlainValues(null);
            // assert
            assertThat(actual).isEmpty();
            // verify
            verifyNoInteractions(repository, policy, crypto, valueSource);
        }

        @Test
        @DisplayName("returns empty set for empty input without hitting repositories")
        void returnsEmptySetForEmtpyInputWithoutHittingRepositories() {
            // arrange
            // conditions
            // act
            Set<InstanceConfigurationKey> actual = writer.replacePlainValues(Map.of());
            // assert
            assertThat(actual).isEmpty();
            // verify
            verifyNoInteractions(repository, policy, crypto, valueSource);
        }

        @Test
        @DisplayName("returns empty set when no stored value change")
        void returnsEmptySetWhenNoStoredValueChange() {
            // arrange
            InstanceConfiguration signup = InstanceConfiguration.create(InstanceConfigurationKey.ENABLE_SIGNUP, "1",
                    InstanceConfigurationCategory.AUTHENTICATION.value(), false);
            Map<InstanceConfigurationKey, String> values = new EnumMap<>(InstanceConfigurationKey.class);
            values.put(InstanceConfigurationKey.ENABLE_SIGNUP, "1");
            // conditions
            when(repository.findByKeyIn(values.keySet())).thenReturn(List.of(signup));
            when(crypto.decryptIfNeeded(same(signup))).thenReturn("1");
            // act
            Set<InstanceConfigurationKey> actual = writer.replacePlainValues(values);
            // assert
            assertThat(actual).isEmpty();
            // verify
            verify(repository, never()).saveAll(anyCollection());
            verify(repository, never()).flush();
        }

        @Test
        @DisplayName("updates changed rows, flushes, and returns changed keys")
        void updatesChangedRowsFlushesAndReturnsChangedKeys() {
            // arrange
            InstanceConfiguration signup = InstanceConfiguration.create(InstanceConfigurationKey.ENABLE_SIGNUP, "1",
                    InstanceConfigurationCategory.AUTHENTICATION.value(), false);
            InstanceConfiguration emailHost = InstanceConfiguration.create(InstanceConfigurationKey.EMAIL_HOST,
                    "smtp.example.com", InstanceConfigurationCategory.SMTP.value(), false);
            Map<InstanceConfigurationKey, String> values = new EnumMap<>(InstanceConfigurationKey.class);
            values.put(InstanceConfigurationKey.ENABLE_SIGNUP, "0");
            values.put(InstanceConfigurationKey.EMAIL_PORT, "smtp.example.com");
            // conditions
            when(repository.findByKeyIn(values.keySet())).thenReturn(List.of(signup, emailHost));
            when(crypto.decryptIfNeeded(same(signup))).thenReturn("1");
            when(crypto.decryptIfNeeded(same(emailHost))).thenReturn("smtp.example.com");
            when(crypto.encryptIfNeeded(false, "0")).thenReturn("0");
            // act
            Set<InstanceConfigurationKey> actual = writer.replacePlainValues(values);
            // assert
            assertThat(actual).containsExactly(InstanceConfigurationKey.ENABLE_SIGNUP,
                    InstanceConfigurationKey.EMAIL_HOST);
            assertThat(signup.getValue()).isEqualTo("0");
            // verify
            verify(repository).saveAll(List.of(signup, emailHost));
            verify(repository).flush();
        }

    }

}
