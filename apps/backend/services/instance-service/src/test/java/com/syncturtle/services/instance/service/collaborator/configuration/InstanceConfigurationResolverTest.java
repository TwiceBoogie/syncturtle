package com.syncturtle.services.instance.service.collaborator.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.model.InstanceConfiguration;
import com.syncturtle.services.instance.repository.InstanceConfigurationRepository;
import com.syncturtle.services.instance.service.param.RequestedKeyParam;
import com.syncturtle.services.instance.type.InstanceConfigurationCategory;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class InstanceConfigurationResolverTest {

    @Mock
    InstanceConfigurationRepository repository;
    @Mock
    InstanceConfigurationPolicy policy;
    @Mock
    InstanceConfigurationCrypto crypto;
    @Mock
    InstanceConfigurationPropertySource valueSource;

    private InstanceConfigurationResolver resolver;

    @BeforeEach
    void setup() {
        resolver = new InstanceConfigurationResolver(repository, policy, crypto, valueSource);
    }

    @Nested
    @DisplayName("resolveValue(RequestedKeyParam)")
    class ResolveValueTests {

        @Test
        @DisplayName("rejects null requested key")
        void rejectsNullRequestedKey() {
            // arrange
            // conditions
            // act + assert
            assertThatThrownBy(() -> resolver.resolveValue((RequestedKeyParam) null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("requested key is required");
            // verify
            verifyNoInteractions(repository, policy, crypto, valueSource);
        }

        @Test
        @DisplayName("resolves db first when skipEnvVar is true")
        void resolvesDbFirstWhenSkipEnvVarIsTrue() {
            // arrange
            InstanceConfiguration row = InstanceConfiguration.create(InstanceConfigurationKey.ENABLE_SIGNUP, "0",
                    InstanceConfigurationCategory.AUTHENTICATION.value(), false);
            RequestedKeyParam requestedKey = RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_SIGNUP);
            // conditions
            when(valueSource.skipEnvVar()).thenReturn(true);
            when(repository.findByKey(InstanceConfigurationKey.ENABLE_SIGNUP)).thenReturn(Optional.of(row));
            when(crypto.decryptIfNeeded(same(row))).thenReturn("0");
            // act
            String actual = resolver.resolveValue(requestedKey);
            // assert
            assertThat(actual).isEqualTo("0");
            // verify
            verify(repository).findByKey(InstanceConfigurationKey.ENABLE_SIGNUP);
            verify(crypto).decryptIfNeeded(same(row));
        }

        @Test
        @DisplayName("resolves db first for derived key even when env vars are allowed")
        void resolvesDbFirstForDerivedKeyEvenWhenEnvVarsAreAllowed() {
            // arrange
            InstanceConfiguration row = InstanceConfiguration.create(InstanceConfigurationKey.IS_GOOGLE_ENABLED, "1",
                    InstanceConfigurationCategory.AUTHENTICATION.value(), false);
            RequestedKeyParam requestedKey = RequestedKeyParam.of(InstanceConfigurationKey.IS_GOOGLE_ENABLED);
            // conditions
            when(valueSource.skipEnvVar()).thenReturn(false);
            when(policy.preferDb(InstanceConfigurationKey.IS_GOOGLE_ENABLED)).thenReturn(true);
            when(repository.findByKey(InstanceConfigurationKey.IS_GOOGLE_ENABLED)).thenReturn(Optional.of(row));
            when(crypto.decryptIfNeeded(same(row))).thenReturn("1");
            // act
            String actual = resolver.resolveValue(requestedKey);
            // assert
            assertThat(actual).isEqualTo("1");
            // verify
        }

        @Test
        @DisplayName("falls back to property value when db first row is missing")
        void fallsbackToPropertyValueWhenDbFirstRowIsMissing() {
            // arrange
            RequestedKeyParam requestedKey = RequestedKeyParam.of(InstanceConfigurationKey.EMAIL_HOST);
            // conditions
            when(valueSource.skipEnvVar()).thenReturn(true);
            when(repository.findByKey(InstanceConfigurationKey.EMAIL_HOST)).thenReturn(Optional.empty());
            when(valueSource.getRaw(InstanceConfigurationKey.EMAIL_HOST)).thenReturn(" smtp.example.com ");
            // act
            String actual = resolver.resolveValue(requestedKey);
            // assert
            assertThat(actual).isEqualTo("smtp.example.com");
            // verify
        }

        @Test
        @DisplayName("resolves properties first when db is not preferred")
        void resolvesPropertiesFirstWhenDbIsNotPreferred() {
            // arrange
            RequestedKeyParam requestedKey = RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_SIGNUP);
            // conditions
            when(valueSource.skipEnvVar()).thenReturn(false);
            when(policy.preferDb(InstanceConfigurationKey.ENABLE_SIGNUP)).thenReturn(false);
            when(valueSource.getRaw(InstanceConfigurationKey.ENABLE_SIGNUP)).thenReturn(" 1 ");
            // act
            String actual = resolver.resolveValue(requestedKey);
            // assert
            assertThat(actual).isEqualTo("1");
            // verify
            verifyNoInteractions(repository, crypto);
        }

        @Test
        @DisplayName("falls back to db row when property value is blank")
        void fallsBackToDbRowWhenPropertyValueIsBlank() {
            // arrange
            InstanceConfiguration row = InstanceConfiguration.create(InstanceConfigurationKey.ENABLE_SIGNUP, "0",
                    InstanceConfigurationCategory.AUTHENTICATION.value(), false);
            RequestedKeyParam requestedKey = RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_SIGNUP);
            // conditions
            when(valueSource.skipEnvVar()).thenReturn(false);
            when(policy.preferDb(InstanceConfigurationKey.ENABLE_SIGNUP)).thenReturn(false);
            when(valueSource.getRaw(InstanceConfigurationKey.ENABLE_SIGNUP)).thenReturn(" ");
            when(repository.findByKey(InstanceConfigurationKey.ENABLE_SIGNUP)).thenReturn(Optional.of(row));
            when(crypto.decryptIfNeeded(same(row))).thenReturn("0");
            // act
            String actual = resolver.resolveValue(requestedKey);
            // assert
            assertThat(actual).isEqualTo("0");
        }

        @Test
        @DisplayName("falls back to policy default when no db row or property value exists")
        void fallsbackToPolicyDefaultWhenNoDbRowOrPropertyValueExists() {
            // arrange
            RequestedKeyParam requestedKey = RequestedKeyParam.of(InstanceConfigurationKey.EMAIL_PORT);
            // conditons
            when(valueSource.skipEnvVar()).thenReturn(false);
            when(policy.preferDb(InstanceConfigurationKey.EMAIL_PORT)).thenReturn(false);
            when(valueSource.getRaw(InstanceConfigurationKey.EMAIL_PORT)).thenReturn(null);
            when(repository.findByKey(InstanceConfigurationKey.EMAIL_PORT)).thenReturn(Optional.empty());
            when(policy.defaultFor(InstanceConfigurationKey.EMAIL_PORT)).thenReturn("587");
            // act
            String actual = resolver.resolveValue(requestedKey);
            // assert
            assertThat(actual).isEqualTo("587");
            // verify
        }

    }

    @Nested
    @DisplayName("resolveRequested(List<RequestedKeyParam>)")
    class ResolveRequestedTests {

        @Test
        @DisplayName("rejects null requested keys")
        void rejectsNullRequestedKeys() {
            assertThatThrownBy(() -> resolver.resolveRequested(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("requested keys are required");
        }

        @Test
        @DisplayName("rejects null requested key entry")
        void rejectsNullRequestedKeyEntry() {
            assertThatThrownBy(() -> resolver.resolveRequested(Arrays.asList((RequestedKeyParam) null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("requested key entry is required");
        }

        @Test
        @DisplayName("resolves requested keys into insertion order map")
        void resolvesRequestedKeysIntoInsertionOrderMap() {
            // arrange
            RequestedKeyParam signup = RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_SIGNUP);
            RequestedKeyParam smtp = RequestedKeyParam.of(InstanceConfigurationKey.ENABLE_SMTP);
            // conditions
            when(valueSource.skipEnvVar()).thenReturn(false);
            when(policy.preferDb(InstanceConfigurationKey.ENABLE_SIGNUP)).thenReturn(false);
            when(policy.preferDb(InstanceConfigurationKey.ENABLE_SMTP)).thenReturn(false);
            when(valueSource.getRaw(InstanceConfigurationKey.ENABLE_SIGNUP)).thenReturn("1");
            when(valueSource.getRaw(InstanceConfigurationKey.ENABLE_SMTP)).thenReturn("0");
            // act
            Map<InstanceConfigurationKey, String> actual = resolver.resolveRequested(List.of(signup, smtp));
            // assert
            assertThat(actual).containsEntry(InstanceConfigurationKey.ENABLE_SIGNUP, "1")
                    .containsEntry(InstanceConfigurationKey.ENABLE_SMTP, "0");
            assertThat(actual.keySet()).containsExactly(InstanceConfigurationKey.ENABLE_SIGNUP,
                    InstanceConfigurationKey.ENABLE_SMTP);
        }

    }

    @Nested
    @DisplayName("ensureMandatorySecretsPresentOrThrow()")
    class EnsureMandatorySecretsPresentOrThrowTests {

        @Test
        @DisplayName("throws when encryption secret key is blank")
        void throwsWhenEncryptionSecretKeyIsBlank() {
            // arrange
            // conditions
            when(valueSource.secretKey()).thenReturn(" ");
            // act + assert
            assertThatThrownBy(() -> resolver.ensureMandatorySecretsPresentOrThrow())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Encryption secret key is required. Configure app.security.encryption.secret-key");
        }

        @Test
        @DisplayName("does not throw when encryption secret key is present")
        void doesNotThrowWhenEncryptionSecretKeyIsPresent() {
            // arrange
            // conditions
            when(valueSource.secretKey()).thenReturn("secret");
            // act
            resolver.ensureMandatorySecretsPresentOrThrow();
            // assert
            // verify
        }

    }

    @Nested
    @DisplayName("parseIntOr(String, int)")
    class ParseIntOrTests {

        @Test
        @DisplayName("returns parsed int when valid")
        void returnsParsedIntWhenValid() {
            assertThat(InstanceConfigurationResolver.parseIntOr(" 2525 ", 587)).isEqualTo(2525);
        }

        @Test
        @DisplayName("returns fallback when blank or invalid")
        void returnsFallbackWhenBlankOrInvalid() {
            assertThat(InstanceConfigurationResolver.parseIntOr(" ", 587)).isEqualTo(587);
            assertThat(InstanceConfigurationResolver.parseIntOr("abc", 587)).isEqualTo(587);
        }

    }

    @Nested
    @DisplayName("isOn(String)")
    class IsOnTests {

        @Test
        @DisplayName("returns true for accepted truthy values")
        void returnsTrueForAcceptedTruthyValues() {
            assertThat(InstanceConfigurationResolver.isOn("1")).isTrue();
            assertThat(InstanceConfigurationResolver.isOn("true")).isTrue();
            assertThat(InstanceConfigurationResolver.isOn("yes")).isTrue();
            assertThat(InstanceConfigurationResolver.isOn("on")).isTrue();
        }

        @Test
        @DisplayName("returns false for blank or non truthy values")
        void returnsFalseForBlankOrNonTruthyValues() {
            assertThat(InstanceConfigurationResolver.isOn(null)).isFalse();
            assertThat(InstanceConfigurationResolver.isOn(" ")).isFalse();
            assertThat(InstanceConfigurationResolver.isOn("0")).isFalse();
            assertThat(InstanceConfigurationResolver.isOn("false")).isFalse();
        }

    }

}
