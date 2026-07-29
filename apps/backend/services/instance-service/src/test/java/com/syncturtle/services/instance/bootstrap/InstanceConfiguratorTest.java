package com.syncturtle.services.instance.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

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

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationWriter;
import com.syncturtle.services.instance.service.param.DerivedFlagParam;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DisplayName("InstanceConfigurator")
class InstanceConfiguratorTest {

    @Mock
    InstanceConfigurationResolver configResolver;
    @Mock
    InstanceConfigurationWriter configWriter;
    @Captor
    ArgumentCaptor<DerivedFlagParam> derivedFlagCaptor;

    private InstanceConfigurator configurator;

    @BeforeEach
    void setup() {
        configurator = new InstanceConfigurator(configResolver, configWriter);
    }

    @Nested
    @DisplayName("run()")
    class RunTests {

        @Test
        @DisplayName("ensures mandatory secrets, seeds managed keys, and seeds derived flags")
        void ensuresMandatorySecretsManagedKeysAndDerivedFlags() {
            // arrange
            // conditions
            // doNothing().when(configResolver).ensureMandatorySecretsPresentOrThrow();
            when(configWriter.seedManagedKeysIfMissing()).thenReturn(7);
            when(configResolver.nonEmpty(any(InstanceConfigurationKey.class)))
                    .thenAnswer(invocation -> {
                        InstanceConfigurationKey key = invocation.getArgument(0);

                        return switch (key) {
                            case GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GITHUB_CLIENT_ID, GITLAB_HOST,
                                    GITLAB_CLIENT_ID, GITLAB_CLIENT_SECRET ->
                                true;
                            case GITHUB_CLIENT_SECRET, INTERCOM_APP_ID -> false;
                            default -> false;
                        };
                    });
            when(configWriter.ensureDerivedFlagIfMissing(any(DerivedFlagParam.class))).thenReturn(true);
            // act
            configurator.run();
            // verify
            InOrder order = inOrder(configResolver, configWriter);
            order.verify(configResolver).ensureMandatorySecretsPresentOrThrow();
            order.verify(configWriter).seedManagedKeysIfMissing();

            verify(configWriter, times(4)).ensureDerivedFlagIfMissing(derivedFlagCaptor.capture());

            List<DerivedFlagParam> derivedFlags = derivedFlagCaptor.getAllValues();

            assertThat(derivedFlags).extracting(DerivedFlagParam::getKey, DerivedFlagParam::isEnabled)
                    .containsExactly(
                            tuple(InstanceConfigurationKey.IS_GOOGLE_ENABLED, true),
                            tuple(InstanceConfigurationKey.IS_GITHUB_ENABLED, false),
                            tuple(InstanceConfigurationKey.IS_GITLAB_ENABLED, true),
                            tuple(InstanceConfigurationKey.IS_INTERCOM_ENABLED, false));

            verify(configResolver).nonEmpty(InstanceConfigurationKey.GOOGLE_CLIENT_ID);
            verify(configResolver).nonEmpty(InstanceConfigurationKey.GOOGLE_CLIENT_SECRET);
            verify(configResolver).nonEmpty(InstanceConfigurationKey.GITHUB_CLIENT_ID);
            verify(configResolver).nonEmpty(InstanceConfigurationKey.GITHUB_CLIENT_SECRET);
            verify(configResolver).nonEmpty(InstanceConfigurationKey.GITLAB_HOST);
            verify(configResolver).nonEmpty(InstanceConfigurationKey.GITLAB_CLIENT_ID);
            verify(configResolver).nonEmpty(InstanceConfigurationKey.GITLAB_CLIENT_SECRET);
        }

        @Test
        @DisplayName("stops before seeding when mandatory secret is missing")
        void stopsBeforeSeedingWhenMandatorySecretIsMissing() {
            // arrange
            // conditions
            doThrow(new IllegalStateException(
                    "Encryption secret key is required. Configure app.security.encryption.secret-key"))
                    .when(configResolver)
                    .ensureMandatorySecretsPresentOrThrow();
            // act + assert
            assertThatThrownBy(() -> configurator.run())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Encryption secret key is required. Configure app.security.encryption.secret-key");
            // verify
            verify(configResolver).ensureMandatorySecretsPresentOrThrow();
            verify(configResolver, never()).nonEmpty(any(InstanceConfigurationKey.class));
            verifyNoInteractions(configWriter);
        }

        @Test
        @DisplayName("counts a derived flag only when writer inserts it")
        void countsDerivedFlagOnlyWhenWriterInsertsIt() {
            // arrange
            // conditions
            when(configWriter.seedManagedKeysIfMissing()).thenReturn(0);
            when(configResolver.nonEmpty(any(InstanceConfigurationKey.class))).thenReturn(false);
            when(configWriter.ensureDerivedFlagIfMissing(any(DerivedFlagParam.class))).thenReturn(false, true, false,
                    true);
            // act
            configurator.run();
            // assert
            // verify
            verify(configWriter, times(4)).ensureDerivedFlagIfMissing(derivedFlagCaptor.capture());

            assertThat(derivedFlagCaptor.getAllValues())
                    .extracting(DerivedFlagParam::getKey, DerivedFlagParam::isEnabled)
                    .containsExactly(
                            tuple(InstanceConfigurationKey.IS_GOOGLE_ENABLED, false),
                            tuple(InstanceConfigurationKey.IS_GITHUB_ENABLED, false),
                            tuple(InstanceConfigurationKey.IS_GITLAB_ENABLED, false),
                            tuple(InstanceConfigurationKey.IS_INTERCOM_ENABLED, false));
        }

    }

}
