package com.syncturtle.platform.services.instance.controllers.internal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.core.dto.response.EmailRuntimeSecretConfigResponse;
import com.syncturtle.common.core.dto.response.UserAuthRuntimeConfigResponse;
import com.syncturtle.common.core.dto.response.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.common.core.dto.response.WorkspaceRuntimeConfigResponse;
import com.syncturtle.platform.services.instance.services.InstanceConfigurationInternalService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/instances")
public class InstanceInternalController {

    private final InstanceConfigurationInternalService service;

    @GetMapping("/configurations/email-config-secrets")
    public EmailRuntimeSecretConfigResponse getEmailRuntimeSecretConfig() {
        return service.getEmailRuntimeSecretConfig();
    }

    @GetMapping("/configurations/user-auth-config")
    public UserAuthRuntimeConfigResponse getUserAuthRuntimeConfig() {
        return service.getUserAuthRuntimeConfig();
    }

    @GetMapping("/configurations/user-auth-config-secrets")
    public UserAuthRuntimeSecretConfigResponse getUserAuthRuntimeSecretConfig() {
        return service.getUserAuthRuntimeSecretConfig();
    }

    @GetMapping("/configurations/workspace-config")
    public WorkspaceRuntimeConfigResponse getWorkspaceRuntimeConfig() {
        return service.getWorkspaceRuntimeConfig();
    }

}
