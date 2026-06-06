package com.syncturtle.services.instance.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.syncturtle.common.core.service.ServiceClientNames;
import com.syncturtle.services.instance.dto.request.InstanceWorkspaceCreateRequest;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceResponse;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceSlugCheckResponse;

@FeignClient(value = ServiceClientNames.WORKSPACE_SERVICE, contextId = "instanceWorkspaceClient", url = "${app.services.workspace-service.base-url}", path = "/internal/v1/instance-workspaces")
public interface WorkspaceClient {

    @GetMapping("/slug-check")
    InstanceWorkspaceSlugCheckResponse checkSlug(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @RequestParam("slug") String slug);

    @PostMapping
    InstanceWorkspaceResponse createWorkspace(
            @RequestHeader("X-Instance-Id") UUID instanceId,
            @RequestBody InstanceWorkspaceCreateRequest request);

}
