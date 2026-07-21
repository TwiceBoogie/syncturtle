package com.syncturtle.services.instance.unit.service.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.service.configuration.InstanceConfigurationPolicy;
import com.syncturtle.services.instance.type.InstanceConfigurationCategory;

@DisplayName("InstanceConfigurationPolicy")
class InstanceConfigurationPolicyTest {

    InstanceConfigurationPolicy policy = new InstanceConfigurationPolicy();

    @Nested
    @DisplayName("managedKeys()")
    class ManagedKeysTests {

        @Test
        @DisplayName("contains setup managed configuration keys")
        void containsSetupManagedConfigurationKeys() {
            assertThat(policy.managedKeys()).contains(
                    InstanceConfigurationKey.ENABLE_SIGNUP,
                    InstanceConfigurationKey.ENABLE_SMTP,
                    InstanceConfigurationKey.EMAIL_HOST_PASSWORD,
                    InstanceConfigurationKey.GOOGLE_CLIENT_ID,
                    InstanceConfigurationKey.GITHUB_CLIENT_SECRET,
                    InstanceConfigurationKey.GITLAB_CLIENT_SECRET,
                    InstanceConfigurationKey.INTERCOM_APP_ID);
        }

    }

    @Nested
    @DisplayName("derivedKeys()")
    class DerivedKeysTests {

        @Test
        @DisplayName("contains computed flags")
        void containsComputeFlags() {
            assertThat(policy.derivedKeys()).containsExactlyInAnyOrder(
                    InstanceConfigurationKey.IS_GOOGLE_ENABLED,
                    InstanceConfigurationKey.IS_GITHUB_ENABLED,
                    InstanceConfigurationKey.IS_GITLAB_ENABLED,
                    InstanceConfigurationKey.IS_INTERCOM_ENABLED);
        }

    }

    @Nested
    @DisplayName("isManaged(InstanceConfigurationKey)")
    class IsManagedTests {

        @Test
        @DisplayName("returns true for managed keys")
        void returnsTrueForManagedKey() {
            assertThat(policy.isManaged(InstanceConfigurationKey.ENABLE_SIGNUP)).isTrue();
        }

        @Test
        @DisplayName("returns false for derived key")
        void returnsFalseForDerivedKey() {
            assertThat(policy.isManaged(InstanceConfigurationKey.IS_GOOGLE_ENABLED)).isFalse();
        }

        @Test
        @DisplayName("rejects null key")
        void rejectsNullKey() {
            assertThatThrownBy(() -> policy.isManaged(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("configuration key is required");
        }

    }

    @Nested
    @DisplayName("isDerived(InstanceConfigurationKey)")
    class IsDerivedTests {

        @Test
        @DisplayName("returns true for derived key")
        void returnsTrueForDerivedKey() {
            assertThat(policy.isDerived(InstanceConfigurationKey.IS_GOOGLE_ENABLED)).isTrue();
        }

        @Test
        @DisplayName("returns false for managed key")
        void returnsFalseForManagedKey() {
            assertThat(policy.isDerived(InstanceConfigurationKey.ENABLE_SIGNUP)).isFalse();
        }

    }

    @Nested
    @DisplayName("preferDb(InstanceConfigurationKey)")
    class PreferDbTests {

        @Test
        @DisplayName("returns true for derived keys")
        void returnsTrueForDerivedKeys() {
            assertThat(policy.preferDb(InstanceConfigurationKey.IS_GITHUB_ENABLED)).isTrue();
        }

        @Test
        @DisplayName("returns false for regular managed keys")
        void returnsFalseForRegularManagedKeys() {
            assertThat(policy.preferDb(InstanceConfigurationKey.ENABLE_SIGNUP)).isFalse();
        }

    }

    @Nested
    @DisplayName("isEncrypted(InstanceConfigurationKey)")
    class IsEncryptedTests {

        @Test
        @DisplayName("returns true for secret keys")
        void returnsTrueForSecretKeys() {
            assertThat(policy.isEncrypted(InstanceConfigurationKey.EMAIL_HOST_PASSWORD)).isTrue();
            assertThat(policy.isEncrypted(InstanceConfigurationKey.GOOGLE_CLIENT_SECRET)).isTrue();
            assertThat(policy.isEncrypted(InstanceConfigurationKey.GITHUB_CLIENT_SECRET)).isTrue();
            assertThat(policy.isEncrypted(InstanceConfigurationKey.GITLAB_CLIENT_SECRET)).isTrue();
            assertThat(policy.isEncrypted(InstanceConfigurationKey.POSTHOG_API_KEY)).isTrue();
        }

        @Test
        @DisplayName("returns false for non secret keys")
        void returnsFalseForNonSecretKeys() {
            assertThat(policy.isEncrypted(InstanceConfigurationKey.EMAIL_HOST)).isFalse();
            assertThat(policy.isEncrypted(InstanceConfigurationKey.ENABLE_SIGNUP)).isFalse();
        }

    }

    @Nested
    @DisplayName("categoryFor(InstanceConfigurationKey)")
    class CategoryForTests {

        @Test
        @DisplayName("returns category for representative keys")
        void returnsCategoryForRepresentativeKeys() {
            assertThat(policy.categoryFor(InstanceConfigurationKey.ENABLE_SIGNUP))
                    .isEqualTo(InstanceConfigurationCategory.AUTHENTICATION.value());
            assertThat(policy.categoryFor(InstanceConfigurationKey.EMAIL_HOST))
                    .isEqualTo(InstanceConfigurationCategory.SMTP.value());
            assertThat(policy.categoryFor(InstanceConfigurationKey.GOOGLE_CLIENT_ID))
                    .isEqualTo(InstanceConfigurationCategory.GOOGLE.value());
            assertThat(policy.categoryFor(InstanceConfigurationKey.GITHUB_CLIENT_ID))
                    .isEqualTo(InstanceConfigurationCategory.GITHUB.value());
            assertThat(policy.categoryFor(InstanceConfigurationKey.GITLAB_CLIENT_ID))
                    .isEqualTo(InstanceConfigurationCategory.GITLAB.value());
            assertThat(policy.categoryFor(InstanceConfigurationKey.INTERCOM_APP_ID))
                    .isEqualTo(InstanceConfigurationCategory.INTERCOM.value());
            assertThat(policy.categoryFor(InstanceConfigurationKey.POSTHOG_API_KEY))
                    .isEqualTo(InstanceConfigurationCategory.ANALYTICS.value());
        }

    }

    @Nested
    @DisplayName("defaultFor(InstanceConfigurationKey)")
    class DefaultForTests {

        @Test
        @DisplayName("returns expected defaults for representative keys")
        void returnsExpectedDefaultsForRepresentativeKeys() {
            assertThat(policy.defaultFor(InstanceConfigurationKey.ENABLE_SIGNUP)).isEqualTo("1");
            assertThat(policy.defaultFor(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN)).isEqualTo("0");
            assertThat(policy.defaultFor(InstanceConfigurationKey.EMAIL_PORT)).isEqualTo("587");
            assertThat(policy.defaultFor(InstanceConfigurationKey.EMAIL_USE_TLS)).isEqualTo("1");
            assertThat(policy.defaultFor(InstanceConfigurationKey.EMAIL_USE_SSL)).isEqualTo("0");
            assertThat(policy.defaultFor(InstanceConfigurationKey.GITLAB_HOST)).isEqualTo("https://gitlab.com");
            assertThat(policy.defaultFor(InstanceConfigurationKey.IS_GOOGLE_ENABLED)).isEqualTo("0");
        }

    }

    @Nested
    @DisplayName("requireManagedKey(InstanceConfigurationKey)")
    class RequireManagedKeyTests {

        @Test
        @DisplayName("allows managed key")
        void allowsManagedKey() {
            policy.requireManagedKey(InstanceConfigurationKey.ENABLE_SIGNUP);
        }

        @Test
        @DisplayName("rejects derived key")
        void rejectsManagedKey() {
            assertThatThrownBy(() -> policy.requireDerivedKey(InstanceConfigurationKey.ENABLE_SIGNUP))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Configuration key is not a derived flag: ENABLE_SIGNUP");
        }

    }

    @Nested
    @DisplayName("requireDerivedKey(InstanceConfigurationKey)")
    class RequireDerivedKeyTests {

        @Test
        @DisplayName("allows derived key")
        void allowsDerivedKey() {
            policy.requireDerivedKey(InstanceConfigurationKey.IS_GOOGLE_ENABLED);
        }

        @Test
        @DisplayName("rejects managed key")
        void rejectsManagedKey() {
            assertThatThrownBy(() -> policy.requireDerivedKey(InstanceConfigurationKey.ENABLE_SIGNUP))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Configuration key is not a derived flag");
        }

    }

}
