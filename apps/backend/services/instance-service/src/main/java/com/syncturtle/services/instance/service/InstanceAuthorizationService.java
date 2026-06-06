package com.syncturtle.services.instance.service;

import java.util.UUID;

import com.syncturtle.services.instance.type.InstanceAdminRole;

public interface InstanceAuthorizationService {
    boolean hasCurrentInstanceRoleAtLeast(UUID userId, InstanceAdminRole minRole);
}
