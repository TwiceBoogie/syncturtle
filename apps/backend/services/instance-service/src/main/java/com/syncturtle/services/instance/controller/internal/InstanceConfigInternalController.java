package com.syncturtle.services.instance.controller.internal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeConfigResponse;
import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.workspace.config.WorkspaceRuntimeConfigResponse;
import com.syncturtle.services.instance.service.InstanceConfigurationInternalService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/instances")
public class InstanceConfigInternalController {

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
