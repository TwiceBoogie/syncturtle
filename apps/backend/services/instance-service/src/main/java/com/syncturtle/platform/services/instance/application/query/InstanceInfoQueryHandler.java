package com.syncturtle.platform.services.instance.application.query;

import java.util.List;

import org.springframework.stereotype.Component;

import com.syncturtle.platform.services.instance.controllers.mappers.InstanceApiMapper;
import com.syncturtle.platform.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.platform.services.instance.dto.response.InstanceInfo;
import com.syncturtle.platform.services.instance.dto.response.InstanceNotConfiguredResponse;
import com.syncturtle.platform.services.instance.dto.response.UserMeResponse;
import com.syncturtle.platform.services.instance.services.InstanceService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstanceInfoQueryHandler {

    private final InstanceService instanceService;
    private final InstanceApiMapper mapper;

    public InstanceInfo getInstanceInfo() {
        return instanceService.instanceInfoAndConfig()
                .map(mapper::toInstanceInfoResponse)
                .orElseGet(InstanceNotConfiguredResponse::new);
    }

    public UserMeResponse getInstanceAdminUserMe() {
        return instanceService.getInstanceAdminUserMe()
                .map(mapper::toInstanceAdminUserMeResponse)
                .orElse(null);
    }

    public List<InstanceAdminResponse> getInstanceAdmins() {
        return mapper.toInstanceAdminResponseList(instanceService.getInstanceAdmins());
    }
}
