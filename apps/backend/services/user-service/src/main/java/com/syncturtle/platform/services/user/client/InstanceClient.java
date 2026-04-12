package com.syncturtle.platform.services.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.syncturtle.common.core.constants.FeignConstants;
import com.syncturtle.common.core.dto.response.UserAuthRuntimeConfigResponse;
import com.syncturtle.common.core.dto.response.UserAuthRuntimeSecretConfigResponse;

@FeignClient(value = FeignConstants.INSTANCE_SERVICE, path = "/internal/v1/instances/configurations")
public interface InstanceClient {

    @GetMapping("/user-auth-config")
    UserAuthRuntimeConfigResponse getUserAuthRuntimeConfig();

    @GetMapping("/user-auth-config-secrets")
    UserAuthRuntimeSecretConfigResponse getUserAuthRuntimeSecretConfig();

}
