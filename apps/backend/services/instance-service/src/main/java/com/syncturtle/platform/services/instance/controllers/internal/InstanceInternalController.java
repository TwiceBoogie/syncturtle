package com.syncturtle.platform.services.instance.controllers.internal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.core.dto.response.EmailRuntimeConfigResponse;
import com.syncturtle.common.web.dto.response.InstanceConfigResponse;
import com.syncturtle.platform.services.instance.services.InstanceConfigurationService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/instances")
public class InstanceInternalController {

    private final InstanceConfigurationService service;

    @GetMapping("/configurations")
    public InstanceConfigResponse getInstanceConfigurations() {
        return service.configurations();
    }

    @GetMapping("/configurations/email-config")
    public EmailRuntimeConfigResponse getRuntimeEmailConfig() {
        return service.getEmailConfigurations();
    }

}
