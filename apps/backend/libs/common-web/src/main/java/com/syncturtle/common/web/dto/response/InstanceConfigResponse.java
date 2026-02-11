package com.syncturtle.common.web.dto.response;

import java.util.Map;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;

import lombok.Data;

@Data
public final class InstanceConfigResponse {
    private Map<InstanceConfigurationKey, String> values;
    private long version;
}
