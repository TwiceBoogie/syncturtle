package com.syncturtle.services.instance.controller.internal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationRequest;
import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationResponse;
import com.syncturtle.services.instance.service.InstanceAdminInternalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/instances")
public class InstanceAuthzInternalController {

    private final InstanceAdminInternalService service;

    @PostMapping("/authz-view")
    public ResolveInstanceAuthorizationResponse resolveAuthz(
            @RequestBody ResolveInstanceAuthorizationRequest request) {
        return service.resolveInstanceAuthz(request);
    }

}
