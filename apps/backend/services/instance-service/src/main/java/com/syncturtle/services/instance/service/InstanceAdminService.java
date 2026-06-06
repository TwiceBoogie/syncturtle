package com.syncturtle.services.instance.service;

import java.util.List;
import java.util.UUID;

import com.syncturtle.services.instance.dto.response.InstanceAdminMeResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminResponse;
import com.syncturtle.services.instance.dto.response.InstanceAdminSessionResponse;

public interface InstanceAdminService {
    List<InstanceAdminResponse> getAdmins();

    InstanceAdminResponse createAdmin(String email);

    void deleteAdmin(UUID currentUserId, UUID instanceAdminId);

    InstanceAdminMeResponse getCurrentAdmin(UUID currentUserId);

    InstanceAdminSessionResponse getSession(UUID currentUserId);
}
