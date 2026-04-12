package com.syncturtle.common.web.dto.response;

import java.util.Map;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public final class InstanceConfigResponse {
    private Map<InstanceConfigurationKey, String> values;
    private long version;
}
