package com.syncturtle.services.instance.service;

import com.syncturtle.services.instance.dto.request.InstanceUpdateRequest;
import com.syncturtle.services.instance.dto.response.InstanceResponse;
import com.syncturtle.services.instance.dto.response.InstanceSetupResponse;

public interface InstanceService {
    InstanceSetupResponse getPublicInstance();

    InstanceResponse instanceUpdate(InstanceUpdateRequest request);

    void markSignupScreenVisited();
}
