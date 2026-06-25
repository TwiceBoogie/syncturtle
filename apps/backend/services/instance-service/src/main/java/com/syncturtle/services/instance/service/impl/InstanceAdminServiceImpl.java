package com.syncturtle.services.instance.service.impl;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;

import com.syncturtle.services.instance.dto.response.InstanceAdminMeResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminSessionResponse;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.InstanceAdmin;
import com.syncturtle.services.instance.repository.InstanceAdminRepository;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.UserRepository;
import com.syncturtle.services.instance.repository.projection.AdminUserDetailLiteProjection;
import com.syncturtle.services.instance.repository.projection.AdminUserDetailsProjection;
import com.syncturtle.services.instance.repository.projection.InstanceAdminProjection;
import com.syncturtle.services.instance.service.InstanceAdminService;
import com.syncturtle.services.instance.service.mapper.InstanceAdminApiMapper;
import com.syncturtle.services.instance.type.InstanceAdminRole;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceAdminServiceImpl implements InstanceAdminService {

    // Repositories
    private final InstanceRepository instanceRepository;
    private final InstanceAdminRepository instanceAdminRepository;
    private final UserRepository userRepository;
    private final InstanceAdminApiMapper instanceAdminApiMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<InstanceAdminResponse> getAdmins() {
        Instance instance = requireConfiguredInstance();

        List<InstanceAdminProjection> instanceAdmins = instanceAdminRepository
                .findByInstance_IdAndDeletedAtIsNull(instance.getId());

        if (instanceAdmins.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = instanceAdmins.stream()
                .map(InstanceAdminProjection::getUserId)
                .distinct()
                .toList();

        Map<UUID, AdminUserDetailLiteProjection> usersById = userRepository
                .findByIdInAndDeletedAtIsNull(userIds)
                .stream()
                .collect(Collectors.toMap(
                        AdminUserDetailLiteProjection::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        return instanceAdmins.stream()
                .map(admin -> instanceAdminApiMapper.toResponse(admin, usersById.get(admin.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public InstanceAdminResponse createAdmin(String email) {
        Assert.hasText(email, "email is required");

        String normalizedEmail = email.toLowerCase().trim();
        Instance instance = requireConfiguredInstance();

        UUID userId = userRepository.findIdByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User does not exist."));

        boolean alreadyAdmin = instanceAdminRepository.existsByInstance_IdAndUserIdAndDeletedAtIsNull(
                instance.getId(),
                userId);

        if (alreadyAdmin) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already an instance admin");
        }

        InstanceAdmin admin = InstanceAdmin.createAdmin(userId, instance);
        InstanceAdmin savedAdmin = instanceAdminRepository.save(admin);

        AdminUserDetailLiteProjection user = userRepository.findByIdAndDeletedAtIsNull(userId);
        return instanceAdminApiMapper.toResponse(savedAdmin, user);
    }

    @Override
    @Transactional
    public void deleteAdmin(UUID currentUserId, UUID instanceAdminId) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(instanceAdminId, "instanceAdminId is required");

        Instance instance = requireConfiguredInstance();

        InstanceAdmin target = instanceAdminRepository.findByIdAndDeletedAtIsNull(instanceAdminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance admin does not exist."));

        if (!target.getInstanceId().equals(instance.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance admin does not exist.");
        }

        if (target.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You cannot remove your own admin access from this endpoint");
        }

        if (target.isAdmin()) {
            long activeAdminCount = instanceAdminRepository.countByInstance_IdAndRoleAndDeletedAtIsNull(
                    instance.getId(),
                    InstanceAdminRole.ADMIN);

            if (activeAdminCount <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot remove the last remaining instance admin.");
            }
        }

        target.revoke(clock);
    }

    @Override
    @Transactional(readOnly = true)
    public InstanceAdminMeResponse getCurrentAdmin(UUID currentUserId) {
        Assert.notNull(currentUserId, "currentUserId is required");

        Instance instance = requireConfiguredInstance();

        AdminUserDetailsProjection admin = instanceAdminRepository
                .findCurrentAdminDetails(instance.getId(), currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Current user is not an instance admin."));

        return instanceAdminApiMapper.toMeResponse(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public InstanceAdminSessionResponse getSession(UUID currentUserId) {
        if (currentUserId == null) {
            return InstanceAdminSessionResponse.anonymous();
        }

        Instance instance = instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElse(null);

        if (instance == null) {
            return InstanceAdminSessionResponse.anonymous();
        }

        return instanceAdminRepository.findCurrentAdminDetails(instance.getId(), currentUserId)
                .map(instanceAdminApiMapper::toSessionResponse)
                .orElseGet(InstanceAdminSessionResponse::anonymous);

    }

    private Instance requireConfiguredInstance() {
        return instanceRepository.findFirstByDeletedAtIsNullOrderByCreatedAtDesc(Instance.class)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Instance is not yet registered."));
    }

}
