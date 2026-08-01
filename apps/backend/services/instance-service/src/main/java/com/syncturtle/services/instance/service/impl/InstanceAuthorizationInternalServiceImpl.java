package com.syncturtle.services.instance.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationRequest;
import com.syncturtle.common.contracts.auth.authz.ResolveInstanceAuthorizationResponse;
import com.syncturtle.services.instance.model.InstanceAdmin;
import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.service.InstanceAuthorizationInternalService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceAuthorizationInternalServiceImpl implements InstanceAuthorizationInternalService {

    private final InstanceAdminRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ResolveInstanceAuthorizationResponse resolveInstanceAuthz(ResolveInstanceAuthorizationRequest request) {
        Assert.notNull(request, "authorization request is required");
        Assert.notNull(request.getInstanceId(), "instanceId is required");
        Assert.notNull(request.getUserId(), "userId is required");

        InstanceAdmin admin = repository
                .findByInstance_IdAndUserIdAndDeletedAtIsNull(request.getInstanceId(), request.getUserId())
                .orElse(null);

        if (admin == null) {
            return ResolveInstanceAuthorizationResponse.builder()
                    .userId(request.getUserId())
                    .instanceId(request.getInstanceId())
                    .instanceAdmin(false)
                    .adminSessionVersion(null)
                    .build();
        }

        return ResolveInstanceAuthorizationResponse.builder()
                .userId(admin.getUserId())
                .instanceId(admin.getInstanceId())
                .instanceAdmin(true)
                .adminSessionVersion(admin.getSessionVersion())
                .build();
    }

}
