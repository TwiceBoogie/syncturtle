package com.syncturtle.services.instance.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.projection.InstanceIdProjection;
import com.syncturtle.services.instance.service.InstanceAuthorizationService;
import com.syncturtle.services.instance.type.InstanceAdminRole;

@Service
public class InstanceAuthorizationServiceImpl implements InstanceAuthorizationService {

    private final InstanceRepository instanceRepository;
    private final InstanceAdminRepository instanceAdminRepository;

    public InstanceAuthorizationServiceImpl(InstanceRepository instanceRepository,
            InstanceAdminRepository instanceAdminRepository) {
        Assert.notNull(instanceRepository, "instanceRepository is required");
        Assert.notNull(instanceAdminRepository, "instanceAdminRepository is required");

        this.instanceRepository = instanceRepository;
        this.instanceAdminRepository = instanceAdminRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasCurrentInstanceRoleAtLeast(UUID userId, InstanceAdminRole requiredRole) {
        Assert.notNull(userId, "userId is required");
        Assert.notNull(requiredRole, "requiredRole is required");

        Optional<UUID> instanceId = findCurrentInstanceId();

        if (instanceId.isEmpty()) {
            return false;
        }

        return instanceAdminRepository
                .findActiveRoleByInstanceIdAndUserId(instanceId.get(), userId)
                .map(actualRole -> actualRole.isAtLeast(requiredRole))
                .orElse(false);
    }

    private Optional<UUID> findCurrentInstanceId() {
        return instanceRepository
                .findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class)
                .map(InstanceIdProjection::getId);
    }
}
