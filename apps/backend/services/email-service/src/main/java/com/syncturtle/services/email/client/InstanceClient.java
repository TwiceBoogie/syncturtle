package com.syncturtle.services.email.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.common.core.service.ServiceClientNames;
import com.syncturtle.services.email.configuration.feign.InstanceServiceFeignConfiguration;

@FeignClient(value = ServiceClientNames.INSTANCE_SERVICE, contextId = "emailInstanceClient", url = "${app.services.instance-service.base-url}", path = "/internal/v1/instances", configuration = InstanceServiceFeignConfiguration.class)
public interface InstanceClient {

    @GetMapping("/configurations/email-config-secrets")
    EmailRuntimeSecretConfigResponse getRuntimeEmailConfig();

}
