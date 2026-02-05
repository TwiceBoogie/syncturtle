package com.syncturtle.platform.services.user.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.syncturtle.common.core.constants.FeignConstants;
import com.syncturtle.common.core.enums.InstanceConfigurationKey;

@FeignClient(value = FeignConstants.INSTANCE_SERVICE, path = "/internal/v1/instances")
public interface InstanceClient {

    @GetMapping("/configurations")
    Map<InstanceConfigurationKey, String> getInstanceConfig();

}
