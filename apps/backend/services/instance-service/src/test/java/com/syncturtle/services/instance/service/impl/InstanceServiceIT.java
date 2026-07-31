package com.syncturtle.services.instance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.web.property.PublicUrlProperties;
import com.syncturtle.common.web.url.PublicUrlResolver;
import com.syncturtle.services.instance.client.UserClient;
import com.syncturtle.services.instance.dto.response.InstanceSetupResponse;
import com.syncturtle.services.instance.mapper.InstanceApiMapper;
import com.syncturtle.services.instance.mapper.InstanceConfigurationApiMapper;
import com.syncturtle.services.instance.messaging.kafka.factory.InstanceEventFactory;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.UserRepository;
import com.syncturtle.services.instance.service.InstanceService;
import com.syncturtle.services.instance.service.collaborator.configuration.InstanceConfigurationResolver;
import com.syncturtle.services.instance.service.collaborator.outbox.InstanceOutboxWriter;
import com.syncturtle.services.instance.service.param.RequestedKeyParam;
import com.syncturtle.services.instance.support.clock.TestClocks;
import com.syncturtle.services.instance.support.fixture.InstanceFixtures;
import com.syncturtle.testing.annotation.JpaIntegrationTest;
import com.syncturtle.testing.annotation.UsePostgresDb;

@JpaIntegrationTest
@UsePostgresDb("instance_service_it")
@Import({ InstanceServiceImpl.class, InstanceEventFactory.class,
        InstanceApiMapper.class,
        InstanceConfigurationApiMapper.class,
        InstanceServiceIT.ServiceTestConfiguration.class })
@DisplayName("InstanceService")
class InstanceServiceIT {

    @Autowired
    InstanceService instanceService;

    @Autowired
    InstanceRepository instanceRepository;

    @Autowired
    InstanceAdminRepository instanceAdminRepository;

    @Autowired
    InstanceEventFactory eventFactory;

    @Autowired
    InstanceApiMapper instanceApiMapper;

    @Autowired
    InstanceConfigurationApiMapper instanceConfigurationApiMapper;

    @Autowired
    PublicUrlProperties properties;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    UserClient userClient;

    @MockitoBean
    PublicUrlResolver hostResolver;

    @MockitoBean
    InstanceConfigurationResolver resolver;

    @MockitoBean
    InstanceOutboxWriter outboxWriter;

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
            Instance instance = InstanceFixtures.activeInstance("Syncturtle");
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

    @EnableJpaAuditing
    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PublicUrlProperties.class)
    static class ServiceTestConfiguration {

        @Bean
        Clock clock() {
            return TestClocks.fixedUtc();
        }

    }

}
