package com.syncturtle.services.instance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeConfigResponse;
import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.workspace.config.WorkspaceRuntimeConfigResponse;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.service.InstanceConfigurationInternalService;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.param.RequestedKeyParam;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("InstanceConfigurationInternalService")
class InstanceConfigurationInternalServiceImplTest {

    @Mock
    InstanceRepository instanceRepository;
    @Mock
    InstanceConfigurationResolver configurationResolver;
    @Captor
    ArgumentCaptor<List<RequestedKeyParam>> requestedKeysCaptor;

    private InstanceConfigurationInternalService service;

    @BeforeEach
    void setup() {
        service = new InstanceConfigurationInternalServiceImpl(instanceRepository, configurationResolver);
    }

    @Nested
    @DisplayName("getEmailRuntimeSecretConfig()")
    class GetEmailRuntimeSecretConfigTests {

        @Test
        @DisplayName("resolves email secret keys and maps runtime response")
        void resolvesEmailSecretKeysAndMapsRuntimeResponse() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            Map<InstanceConfigurationKey, String> values = new EnumMap<>(InstanceConfigurationKey.class);
            values.put(InstanceConfigurationKey.ENABLE_SMTP, "1");
            values.put(InstanceConfigurationKey.EMAIL_HOST, "smtp.example.com");
            values.put(InstanceConfigurationKey.EMAIL_HOST_USER, "mailer");
            values.put(InstanceConfigurationKey.EMAIL_HOST_PASSWORD, "secret");
            values.put(InstanceConfigurationKey.EMAIL_PORT, "2525");
            values.put(InstanceConfigurationKey.EMAIL_FROM, "noreply@example.com");
            values.put(InstanceConfigurationKey.EMAIL_USE_TLS, "true");
            values.put(InstanceConfigurationKey.EMAIL_USE_SSL, "0");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(configurationResolver.resolveRequested(anyList())).thenReturn(values);
            // act
            EmailRuntimeSecretConfigResponse actual = service.getEmailRuntimeSecretConfig();
            // assert
            assertThat(actual.isEnabled()).isTrue();
            assertThat(actual.getHost()).isEqualTo("smtp.example.com");
            assertThat(actual.getUsername()).isEqualTo("mailer");
            assertThat(actual.getPassword()).isEqualTo("secret");
            assertThat(actual.getPort()).isEqualTo(2525);
            assertThat(actual.getFrom()).isEqualTo("noreply@example.com");
            assertThat(actual.isUseTls()).isTrue();
            assertThat(actual.isUseSsl()).isFalse();
            assertThat(actual.getVersion()).isEqualTo(instance.getConfig().getVersion());
            // verify
            verify(configurationResolver).resolveRequested(requestedKeysCaptor.capture());

            assertThat(requestedKeysCaptor.getValue()).extracting(RequestedKeyParam::getKey)
                    .containsExactly(
                            InstanceConfigurationKey.ENABLE_SMTP,
                            InstanceConfigurationKey.EMAIL_HOST,
                            InstanceConfigurationKey.EMAIL_HOST_USER,
                            InstanceConfigurationKey.EMAIL_HOST_PASSWORD,
                            InstanceConfigurationKey.EMAIL_PORT,
                            InstanceConfigurationKey.EMAIL_FROM,
                            InstanceConfigurationKey.EMAIL_USE_TLS,
                            InstanceConfigurationKey.EMAIL_USE_SSL);
        }

        @Test
        @DisplayName("uses default smtp port when configured value is invalid")
        void usesDefaultSmtpPortWhenConfiguredValueIsInvalid() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            Map<InstanceConfigurationKey, String> values = new EnumMap<>(InstanceConfigurationKey.class);
            values.put(InstanceConfigurationKey.EMAIL_PORT, "not-a-number");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(configurationResolver.resolveRequested(anyList())).thenReturn(values);
            // act
            EmailRuntimeSecretConfigResponse actual = service.getEmailRuntimeSecretConfig();
            // assert
            assertThat(actual.getPort()).isEqualTo(587);
        }

    }

    @Nested
    @DisplayName("getUserAuthRuntimeConfig()")
    class GetUserAuthRuntimeConfig {

        @Test
        @DisplayName("resolves auth runtime keys and maps booleans")
        void resolvesAuthRuntimeKeysAndMapsBooleans() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            Map<InstanceConfigurationKey, String> values = new EnumMap<>(InstanceConfigurationKey.class);
            values.put(InstanceConfigurationKey.ENABLE_SIGNUP, "1");
            values.put(InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN, "yes");
            values.put(InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD, "true");
            values.put(InstanceConfigurationKey.ENABLE_SMTP, "on");
            values.put(InstanceConfigurationKey.IS_GOOGLE_ENABLED, "0");
            values.put(InstanceConfigurationKey.IS_GITHUB_ENABLED, "false");
            values.put(InstanceConfigurationKey.IS_GITLAB_ENABLED, "");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(configurationResolver.resolveRequested(anyList())).thenReturn(values);
            // act
            UserAuthRuntimeConfigResponse actual = service.getUserAuthRuntimeConfig();
            // assert
            assertThat(actual.isSignupEnabled()).isTrue();
            assertThat(actual.isMagicLinkEnabled()).isTrue();
            assertThat(actual.isEmailPasswordEnabled()).isTrue();
            assertThat(actual.isSmtpEnabled()).isTrue();
            assertThat(actual.isGoogleEnabled()).isFalse();
            assertThat(actual.isGithubEnabled()).isFalse();
            assertThat(actual.isGitlabEnabled()).isFalse();
            assertThat(actual.getVersion()).isEqualTo(instance.getConfig().getVersion());
            // verify
            verify(configurationResolver).resolveRequested(requestedKeysCaptor.capture());

            assertThat(requestedKeysCaptor.getValue()).extracting(RequestedKeyParam::getKey)
                    .containsExactly(
                            InstanceConfigurationKey.ENABLE_SIGNUP,
                            InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN,
                            InstanceConfigurationKey.ENABLE_EMAIL_PASSWORD,
                            InstanceConfigurationKey.ENABLE_SMTP,
                            InstanceConfigurationKey.IS_GOOGLE_ENABLED,
                            InstanceConfigurationKey.IS_GITHUB_ENABLED,
                            InstanceConfigurationKey.IS_GITLAB_ENABLED);
        }

    }

    @Nested
    @DisplayName("getUserAuthRuntimeSecretConfig()")
    class GetUserAuthRuntimeSecretConfig {

        @Test
        @DisplayName("resolves user auth secret keys and maps response")
        void resolvesUserAuthSecretKeyAndMapsResponse() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            Map<InstanceConfigurationKey, String> values = new EnumMap<>(InstanceConfigurationKey.class);
            values.put(InstanceConfigurationKey.GOOGLE_CLIENT_ID, "google-client-id");
            values.put(InstanceConfigurationKey.GOOGLE_CLIENT_SECRET, "google-client-secret");
            values.put(InstanceConfigurationKey.GITHUB_CLIENT_ID, "github-client-id");
            values.put(InstanceConfigurationKey.GITHUB_CLIENT_SECRET, "github-client-secret");
            values.put(InstanceConfigurationKey.GITHUB_APP_NAME, "syncturtle-dev");
            values.put(InstanceConfigurationKey.GITLAB_HOST, "https://gitlab.com");
            values.put(InstanceConfigurationKey.GITLAB_CLIENT_ID, "gitlab-client-id");
            values.put(InstanceConfigurationKey.GITLAB_CLIENT_SECRET, "gitlab-client-secret");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(configurationResolver.resolveRequested(anyList())).thenReturn(values);
            // act
            UserAuthRuntimeSecretConfigResponse actual = service.getUserAuthRuntimeSecretConfig();
            // assert
            assertThat(actual.getGoogleClientId()).isEqualTo("google-client-id");
            assertThat(actual.getGoogleClientSecret()).isEqualTo("google-client-secret");
            assertThat(actual.getGithubClientId()).isEqualTo("github-client-id");
            assertThat(actual.getGithubClientSecret()).isEqualTo("github-client-secret");
            assertThat(actual.getGithubAppName()).isEqualTo("syncturtle-dev");
            assertThat(actual.getGitlabHost()).isEqualTo("https://gitlab.com");
            assertThat(actual.getGitlabClientId()).isEqualTo("gitlab-client-id");
            assertThat(actual.getGitlabClientSecret()).isEqualTo("gitlab-client-secret");
        }

    }

    @Nested
    @DisplayName("getWorkspaceRuntimeConfig()")
    class GetWorkspaceRuntimeConfig {

        @Test
        @DisplayName("resolves workspace runtime keys and maps response")
        void resolvesWorkspaceRuntimeKeysAndMapsResponse() {
            // arrange
            Instance instance = InstanceFixtures.persistedInstance("Syncturtle");
            Map<InstanceConfigurationKey, String> values = new EnumMap<>(InstanceConfigurationKey.class);
            values.put(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION, "1");
            // conditions
            when(instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class))
                    .thenReturn(Optional.of(instance));
            when(configurationResolver.resolveRequested(anyList())).thenReturn(values);
            // act
            WorkspaceRuntimeConfigResponse actual = service.getWorkspaceRuntimeConfig();
            // assert
            assertThat(actual.isWorkspaceCreationDisabled()).isTrue();
            assertThat(actual.getVersion()).isEqualTo(instance.getConfig().getVersion());
            // verify
            verify(configurationResolver).resolveRequested(requestedKeysCaptor.capture());

            assertThat(requestedKeysCaptor.getValue()).extracting(RequestedKeyParam::getKey)
                    .containsExactly(InstanceConfigurationKey.DISABLE_WORKSPACE_CREATION);
        }

    }

}
