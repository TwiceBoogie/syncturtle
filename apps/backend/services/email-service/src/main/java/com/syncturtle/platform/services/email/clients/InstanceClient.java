package com.syncturtle.platform.services.email.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.syncturtle.common.core.constants.FeignConstants;
import com.syncturtle.common.core.dto.response.EmailRuntimeConfigResponse;

@FeignClient(value = FeignConstants.INSTANCE_SERVICE, path = "/internal/v1/instances")
public interface InstanceClient {

    @GetMapping("/configurations/email-config")
    EmailRuntimeConfigResponse getRuntimeEmailConfig();

}
