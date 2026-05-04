package com.syncturtle.services.instance.services.authz;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.syncturtle.services.instance.repositories.InstanceAdminRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstanceAuthorizationService {

    private final InstanceAdminRepository instanceAdminRepository;

    public boolean isInstanceAdmin(UUID userId, int minRole) {
        if (userId == null) {
            return false;
        }
        return instanceAdminRepository.existsByUserIdAndRoleGreaterThanEqual(userId, minRole);
    }
}
