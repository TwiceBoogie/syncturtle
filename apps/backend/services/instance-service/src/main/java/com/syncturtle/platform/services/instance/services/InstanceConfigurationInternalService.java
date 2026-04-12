package com.syncturtle.platform.services.instance.services;

import com.syncturtle.common.core.dto.response.EmailRuntimeSecretConfigResponse;
import com.syncturtle.common.core.dto.response.UserAuthRuntimeConfigResponse;
import com.syncturtle.common.core.dto.response.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.common.core.dto.response.WorkspaceRuntimeConfigResponse;

public interface InstanceConfigurationInternalService {
    EmailRuntimeSecretConfigResponse getEmailRuntimeSecretConfig();

    UserAuthRuntimeConfigResponse getUserAuthRuntimeConfig();

    UserAuthRuntimeSecretConfigResponse getUserAuthRuntimeSecretConfig();

    WorkspaceRuntimeConfigResponse getWorkspaceRuntimeConfig();
}
