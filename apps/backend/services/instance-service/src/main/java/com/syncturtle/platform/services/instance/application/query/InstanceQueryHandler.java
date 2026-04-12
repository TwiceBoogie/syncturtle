package com.syncturtle.platform.services.instance.application.query;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.controllers.mappers.InstanceApiMapper;
import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSigninResult;
import com.syncturtle.platform.services.instance.dto.internal.InstanceAdminSignupResult;
import com.syncturtle.platform.services.instance.dto.request.InstanceAdminSigninForm;
import com.syncturtle.platform.services.instance.dto.request.InstanceAdminSignupForm;
import com.syncturtle.platform.services.instance.dto.request.InstanceRequest;
import com.syncturtle.platform.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.platform.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.platform.services.instance.dto.response.InstanceInfo;
import com.syncturtle.platform.services.instance.dto.response.InstanceNotConfiguredResponse;
import com.syncturtle.platform.services.instance.dto.response.InstanceResponse;
import com.syncturtle.platform.services.instance.dto.response.UserMeResponse;
import com.syncturtle.platform.services.instance.payload.InstanceSummary;
import com.syncturtle.platform.services.instance.services.InstanceConfigurationService;
import com.syncturtle.platform.services.instance.services.InstanceService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstanceQueryHandler {

    private final InstanceService instanceService;
    private final InstanceConfigurationService instanceConfigurationService;
    private final InstanceApiMapper mapper;

    public InstanceInfo getInstanceInfo() {
        return instanceService.instanceInfoAndConfig()
                .map(mapper::toInstanceInfoResponse)
                .orElseGet(InstanceNotConfiguredResponse::new);
    }

    public InstanceResponse instanceUpdate(InstanceRequest request) {
        InstanceSummary instance = instanceService.instanceUpdate(request);
        return mapper.toInstanceResponse(instance);
    }

    public UserMeResponse getInstanceAdminUserMe() {
        return instanceService.getInstanceAdminUserMe()
                .map(mapper::toInstanceAdminUserMeResponse)
                .orElse(null);
    }

    public List<InstanceAdminResponse> getInstanceAdmins() {
        return mapper.toInstanceAdminResponseList(instanceService.getInstanceAdmins());
    }

    public InstanceAdminSignupResult instanceAdminSignup(InstanceAdminSignupForm form) {
        return instanceService.instanceAdminSignup(form);
    }

    public InstanceAdminSigninResult instanceAdminSignin(InstanceAdminSigninForm form) {
        return instanceService.instanceAdminSignin(form);
    }

    public List<InstanceConfigurationResponse> configurationsAll() {
        return instanceConfigurationService.configurationsAll();
    }

    public List<InstanceConfigurationResponse> configurationUpdate(
            Map<InstanceConfigurationKey, String> configurations) {
        return instanceConfigurationService.configurationsUpdate(configurations);
    }

    public void disableEmail() {
        instanceConfigurationService.disableEmail();
    }

}
