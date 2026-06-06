package com.syncturtle.services.workspace.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import com.syncturtle.common.contracts.workspace.config.WorkspaceRuntimeConfigResponse;
import com.syncturtle.common.core.service.ServiceClientNames;

@FeignClient(value = ServiceClientNames.INSTANCE_SERVICE, contextId = "workspaceInstanceClient", url = "${app.services.instance-service.base-url}", path = "/internal/v1/instances")
public interface InstanceClient {

    @GetMapping("/configurations/workspace-config")
    public WorkspaceRuntimeConfigResponse getWorkspaceRuntimeConfigResponse();

}
