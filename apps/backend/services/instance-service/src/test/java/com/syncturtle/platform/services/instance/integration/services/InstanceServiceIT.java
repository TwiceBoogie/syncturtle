package com.syncturtle.platform.services.instance.integration.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.core.enums.InstanceEdition;
import com.syncturtle.common.core.utils.StringHelper;
import com.syncturtle.common.spring.web.url.HostUrlBuilder;
import com.syncturtle.platform.services.instance.client.UserClient;
import com.syncturtle.platform.services.instance.models.Instance;
import com.syncturtle.platform.services.instance.payload.BinaryMetadata;
import com.syncturtle.platform.services.instance.payload.RegistrationSpec;
import com.syncturtle.platform.services.instance.payload.RuntimeMetadata;
import com.syncturtle.platform.services.instance.repositories.InstanceAdminRepository;
import com.syncturtle.platform.services.instance.repositories.InstanceInfoAggregate;
import com.syncturtle.platform.services.instance.repositories.InstanceRepository;
import com.syncturtle.platform.services.instance.repositories.UserRepository;
import com.syncturtle.platform.services.instance.services.InstanceService;
import com.syncturtle.platform.services.instance.services.configuration.InstanceConfigurationResolver;
import com.syncturtle.testing.annotations.IntegrationTest;
import com.syncturtle.testing.annotations.UsePostgresDb;

@IntegrationTest
@UsePostgresDb("instance_service_it")
public class InstanceServiceIT {

    @Autowired
    InstanceService instanceService;

    @Autowired
    InstanceRepository instanceRepository;

    @Autowired
    InstanceAdminRepository instanceAdminRepository;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    UserClient userClient;

    @MockitoBean
    HostUrlBuilder hostUrlBuilder;

    @MockitoBean
    InstanceConfigurationResolver resolver;

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
        instanceRepository.deleteAll();
    }

    @Nested
    class InstanceInfoAndConfigTests {

        @Test
        void whenInstanceExists_returnsAggregate_fromDb_andConfigurations() {
            // Arrange
            Instance instance = generateInstance();
            instanceRepository.save(instance);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<InstanceConfigurationResolver.RequestedKey>> captor = ArgumentCaptor
                    .forClass(List.class);
            when(resolver.resolveRequested(captor.capture())).thenReturn(Map.of(InstanceConfigurationKey.ENABLE_SIGNUP,
                    "1", InstanceConfigurationKey.POSTHOG_HOST, "https://posthog.local"));

            // Act
            Optional<InstanceInfoAggregate> result = instanceService.instanceInfoAndConfig();
            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getUserCount()).isEqualTo(0);
            // Verify
            verify(resolver).resolveRequested(anyList());
            List<InstanceConfigurationResolver.RequestedKey> requested = captor.getValue();
            assertThat(requested).extracting(InstanceConfigurationResolver.RequestedKey::key).contains(
                    InstanceConfigurationKey.ENABLE_SIGNUP, InstanceConfigurationKey.POSTHOG_HOST,
                    InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN);
        }

    }

    private static Instance generateInstance() {
        Instant now = Instant.now();
        RuntimeMetadata runtimeMetadata = new RuntimeMetadata("", "", "");
        BinaryMetadata binaryMetadata = BinaryMetadata.from(null, now);
        RegistrationSpec spec = new RegistrationSpec(UUID.randomUUID().toString(),
                StringHelper.defaultInstanceName(InstanceEdition.COMMUNITY), InstanceEdition.COMMUNITY, true, true,
                true, runtimeMetadata, binaryMetadata);

        Instance instance = new Instance();
        instance.initializeForRegistration(spec, now);
        return instance;
    }

}
