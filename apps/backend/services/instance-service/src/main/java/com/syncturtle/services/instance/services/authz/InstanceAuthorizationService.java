package com.syncturtle.services.instance.services.authz;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncturtle.services.instance.repositories.InstanceAdminRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceAuthorizationService {

    private final InstanceAdminRepository instanceAdminRepository;

    @Transactional(readOnly = true)
    public boolean hasInstanceRoleAtLeast(UUID userId, int minRole) {
        if (userId == null) {
            return false;
        }
        if (minRole < 0) {
            return false;
        }
        return instanceAdminRepository.existsByUserIdAndRoleGreaterThanEqual(userId, minRole);
    }
}
