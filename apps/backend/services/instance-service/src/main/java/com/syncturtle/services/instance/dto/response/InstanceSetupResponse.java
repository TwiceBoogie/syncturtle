package com.syncturtle.services.instance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceSetupResponse {
    Boolean isActivated;
    Boolean isSetupDone;
    InstanceSetupConfigResponse config;
    InstanceResponse instance;
}
