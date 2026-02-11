package com.syncturtle.platform.services.instance.controllers.internal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.web.dto.response.InstanceConfigResponse;
import com.syncturtle.platform.services.instance.services.InstanceService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/instances")
public class InstanceInternalController {

    @SuppressWarnings("unused")
    private final InstanceService instanceService;

    @GetMapping("/configurations")
    public InstanceConfigResponse getInstanceConfigurations() {
        return new InstanceConfigResponse();
    }

}
