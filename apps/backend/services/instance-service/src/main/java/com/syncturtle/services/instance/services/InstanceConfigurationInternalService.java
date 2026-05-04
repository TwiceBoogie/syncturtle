package com.syncturtle.services.instance.services;

import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeConfigResponse;
import com.syncturtle.common.contracts.auth.config.UserAuthRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.common.contracts.workspace.config.WorkspaceRuntimeConfigResponse;

public interface InstanceConfigurationInternalService {
    EmailRuntimeSecretConfigResponse getEmailRuntimeSecretConfig();

    UserAuthRuntimeConfigResponse getUserAuthRuntimeConfig();

    UserAuthRuntimeSecretConfigResponse getUserAuthRuntimeSecretConfig();

    WorkspaceRuntimeConfigResponse getWorkspaceRuntimeConfig();
}
