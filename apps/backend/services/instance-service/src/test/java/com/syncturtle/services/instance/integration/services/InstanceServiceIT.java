package com.syncturtle.services.instance.integration.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.instance.client.UserClient;
import com.syncturtle.services.instance.dto.response.InstanceSetupResponse;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.param.InstanceBinaryParam;
import com.syncturtle.services.instance.model.param.InstanceRegistrationParam;
import com.syncturtle.services.instance.model.param.InstanceRuntimeParam;
import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.UserRepository;
import com.syncturtle.services.instance.service.InstanceService;
import com.syncturtle.services.instance.service.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.configuration.param.RequestedKeyParam;
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
    PublicUrlResolver hostResolver;

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
            ArgumentCaptor<List<RequestedKeyParam>> captor = ArgumentCaptor
                    .forClass(List.class);
            when(resolver.resolveRequested(captor.capture())).thenReturn(Map.of(InstanceConfigurationKey.ENABLE_SIGNUP,
                    "1", InstanceConfigurationKey.POSTHOG_HOST, "https://posthog.local"));

            // Act
            InstanceSetupResponse result = instanceService.getPublicInstance();
            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getInstance().isActivated()).isTrue();
            // Verify
            verify(resolver).resolveRequested(anyList());
            List<RequestedKeyParam> requested = captor.getValue();
            assertThat(requested).extracting(item -> item.getKey())
                    .containsExactlyInAnyOrder(
                            InstanceConfigurationKey.ENABLE_SIGNUP,
                            InstanceConfigurationKey.POSTHOG_HOST,
                            InstanceConfigurationKey.ENABLE_MAGIC_LINK_LOGIN);
        }

    }

    private static Instance generateInstance() {
        Instant now = Instant.now();
        InstanceRuntimeParam runtimeMetadata = InstanceRuntimeParam.empty();
        InstanceBinaryParam binaryMetadata = InstanceBinaryParam.from(null, now);
        InstanceRegistrationParam param = InstanceRegistrationParam.builder()
                .runtime(runtimeMetadata)
                .binary(binaryMetadata)
                .build();

        return Instance.register(param);
    }

}
