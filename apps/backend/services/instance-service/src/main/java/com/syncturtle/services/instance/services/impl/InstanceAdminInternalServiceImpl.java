package com.syncturtle.services.instance.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationRequest;
import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationResponse;
import com.syncturtle.services.instance.models.InstanceAdmin;
import com.syncturtle.services.instance.repositories.InstanceAdminRepository;
import com.syncturtle.services.instance.services.InstanceAdminInternalService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceAdminInternalServiceImpl implements InstanceAdminInternalService {

    private final InstanceAdminRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ResolveInstanceAuthorizationResponse resolveInstanceAuthz(ResolveInstanceAuthorizationRequest request) {
        InstanceAdmin instanceAdmin = repository
                .findByInstance_IdAndUserId(request.getInstanceId(), request.getUserId())
                .orElse(null);

        boolean instanceAdminFlag = instanceAdmin != null;
        Long adminSessionVersion = instanceAdminFlag ? instanceAdmin.getSessionVersion() : null;

        return ResolveInstanceAuthorizationResponse.builder()
                .userId(instanceAdmin.getUserId())
                .instanceId(instanceAdmin.getInstanceId())
                .instanceAdmin(instanceAdminFlag)
                .adminSessionVersion(adminSessionVersion)
                .build();
    }

}
