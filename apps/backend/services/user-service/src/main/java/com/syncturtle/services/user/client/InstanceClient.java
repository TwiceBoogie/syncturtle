package com.syncturtle.services.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationRequest;
import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationResponse;
import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeConfigResponse;
import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.common.core.service.ServiceClientNames;

@FeignClient(value = ServiceClientNames.INSTANCE_SERVICE, contextId = "userInstanceClient", url = "${app.services.instance-service.base-url}", path = "/internal/v1/instances")
public interface InstanceClient {

    @GetMapping("/configurations/user-auth-config")
    UserAuthRuntimeConfigResponse getUserAuthRuntimeConfig();

    @GetMapping("/configurations/user-auth-config-secrets")
    UserAuthRuntimeSecretConfigResponse getUserAuthRuntimeSecretConfig();

    @PostMapping("/authz-view")
    ResolveInstanceAuthorizationResponse resolveAuthz(
            @RequestBody ResolveInstanceAuthorizationRequest request);

}
