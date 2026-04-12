package com.syncturtle.platform.services.instance.controllers.mappers;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.common.web.dto.response.InstanceConfigResponse;

@Component
public class InstanceConfigurationApiMapper {

    public InstanceConfigResponse toInstanceConfigResponse(Map<InstanceConfigurationKey, String> configurations,
            long version) {
        return new InstanceConfigResponse(configurations, version);
    }

}
