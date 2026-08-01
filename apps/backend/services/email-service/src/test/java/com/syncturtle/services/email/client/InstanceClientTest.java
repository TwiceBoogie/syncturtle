package com.syncturtle.services.email.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.common.core.service.ServiceClientNames;
import com.syncturtle.services.email.configuration.feign.InstanceServiceFeignConfiguration;

@DisplayName("InstanceClient")
class InstanceClientTest {

    @Nested
    @DisplayName("getRuntimeEmailConfig()")
    class GetRuntimeEmailConfigTests {

        @Test
        @DisplayName("preserves the internal feign service, path, and response contract")
        void preservesInternalFeignContract() throws NoSuchMethodException {
            FeignClient client = InstanceClient.class.getAnnotation(FeignClient.class);
            Method method = InstanceClient.class.getMethod("getRuntimeEmailConfig");
            GetMapping mapping = method.getAnnotation(GetMapping.class);

            assertThat(client.value()).isEqualTo(ServiceClientNames.INSTANCE_SERVICE);
            assertThat(client.contextId()).isEqualTo("emailInstanceClient");
            assertThat(client.url()).isEqualTo("${app.clients.instance-service.base-url}");
            assertThat(client.path()).isEqualTo("/internal/v1/instances");
            assertThat(client.configuration()).containsExactly(InstanceServiceFeignConfiguration.class);
            assertThat(mapping.value()).containsExactly("/configurations/email-config-secrets");
            assertThat(method.getReturnType()).isEqualTo(EmailRuntimeSecretConfigResponse.class);
        }

    }

}
