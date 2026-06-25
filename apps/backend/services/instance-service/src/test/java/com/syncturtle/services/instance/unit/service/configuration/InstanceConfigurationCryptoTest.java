package com.syncturtle.services.instance.unit.service.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jasypt.encryption.StringEncryptor;
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
import com.syncturtle.services.instance.service.configuration.InstanceConfigurationCrypto;
import com.syncturtle.services.instance.type.InstanceConfigurationCategory;

@DisplayName("InstanceConfigurationCrypto")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class InstanceConfigurationCryptoTest {

    @Mock
    StringEncryptor encryptor;

    InstanceConfigurationCrypto crypto;

    @BeforeEach
    void setup() {
        crypto = new InstanceConfigurationCrypto(encryptor);
    }

    @Nested
    @DisplayName("decryptIfNeeded(InstanceConfiguration)")
    class DecryptIfNeededTests {

        @Test
        @DisplayName("rejects null row")
        void rejectsNullRow() {
            assertThatThrownBy(() -> crypto.decryptIfNeeded(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("configuration row is required");
        }

        @Test
        @DisplayName("returns trimmed stored value when row is not encrypted")
        void returnsTrimmedStoredValueWhenRowIsNotEncrypted() {
            // arrange
            InstanceConfiguration row = InstanceConfiguration.create(InstanceConfigurationKey.EMAIL_HOST,
                    " smtp.example.com ", InstanceConfigurationCategory.SMTP.value(), false);
            // conditions
            // act
            String actual = crypto.decryptIfNeeded(row);
            // assert
            assertThat(actual).isEqualTo("smtp.example.com");
            // verify
            verify(encryptor, never()).decrypt(anyString());
        }

        @Test
        @DisplayName("returns empty string for encrypted blank value without decrypting")
        void returnsEmptyStringForEncryptedBlankValueWithoutDecrypting() {
            // arrange
            InstanceConfiguration row = InstanceConfiguration.create(InstanceConfigurationKey.EMAIL_HOST_PASSWORD,
                    " ", InstanceConfigurationCategory.SMTP.value(), true);
            // conditions
            // act
            String actual = crypto.decryptIfNeeded(row);
            // assert
            assertThat(actual).isEmpty();
            // verify
            verify(encryptor, never()).decrypt(anyString());
        }

        @Test
        @DisplayName("decrypts encrypted non blank value")
        void decryptsEncryptedNonBlankValue() {
            // arrange
            InstanceConfiguration row = InstanceConfiguration.create(InstanceConfigurationKey.EMAIL_HOST_PASSWORD,
                    " encrypted ", InstanceConfigurationCategory.SMTP.value(), true);
            // conditions
            when(encryptor.decrypt("encrypted")).thenReturn("plain-secret");
            // act
            String actual = crypto.decryptIfNeeded(row);
            // assert
            assertThat(actual).isEqualTo("plain-secret");
            // verify
            verify(encryptor).decrypt("encrypted");
        }

    }

    @Nested
    @DisplayName("encryptIfNeeded(boolean, String)")
    class EncryptIfNeededTests {

        @Test
        @DisplayName("returns trimmed value when encrypted is false")
        void returnsTrimmedValueWhenEncryptedIsFalse() {
            // arrange
            // conditions
            // act
            String actual = crypto.encryptIfNeeded(false, " value ");
            // assert
            assertThat(actual).isEqualTo("value");
            // verify
            verify(encryptor, never()).encrypt(anyString());
        }

        @Test
        @DisplayName("returns empty string for encrypted blank value without encrypting")
        void returnsEmptyStringForEncryptedBlankValueWithoutEncrypting() {
            // arrange
            // conditions
            // act
            String actual = crypto.encryptIfNeeded(true, " ");
            // assert
            assertThat(actual).isEmpty();
            // verify
            verify(encryptor, never()).encrypt(anyString());
        }

        @Test
        @DisplayName("encrypts normalized non blank value")
        void encryptsNormalizedNonBlankValue() {
            // arrange
            // conditions
            when(encryptor.encrypt("plain-secret")).thenReturn("encrypted-secret");
            // act
            String actual = crypto.encryptIfNeeded(true, " plain-secret ");
            // assert
            assertThat(actual).isEqualTo("encrypted-secret");
            // verify
            verify(encryptor).encrypt("plain-secret");
        }

    }

    @Nested
    @DisplayName("wouldStoreValueChange(InstanceConfiguration, String)")
    class WouldStoredValueChangeTests {

        @Test
        @DisplayName("returns false when normalized plain values are equal")
        void returnsFalseWhenNormalizedPlainValuesAreEqual() {
            // arrange
            InstanceConfiguration row = InstanceConfiguration.create(InstanceConfigurationKey.EMAIL_HOST,
                    "smtp.example.com", InstanceConfigurationCategory.SMTP.value(), false);
            // conditions
            // act
            boolean actual = crypto.wouldStoredValueChange(row, " smtp.example.com ");
            // assert
            assertThat(actual).isFalse();
            // verify
        }

        @Test
        @DisplayName("returns true when normalized plain values differ")
        void returnsTrueWhenNormalizedPlainValuesDiffer() {
            // arrange
            InstanceConfiguration row = InstanceConfiguration.create(InstanceConfigurationKey.EMAIL_HOST,
                    "smtp.example.com", InstanceConfigurationCategory.SMTP.value(), false);
            // conditions
            // act
            boolean actual = crypto.wouldStoredValueChange(row, "smtp2.example.com");
            // assert
            assertThat(actual).isTrue();
            // verify
        }

    }

}
