package com.syncturtle.platform.services.workspace.integration.services;

import org.springframework.beans.factory.annotation.Autowired;

import com.syncturtle.platform.services.workspace.repositories.WorkspaceMemberRepository;
import com.syncturtle.platform.services.workspace.repositories.WorkspaceRepository;
import com.syncturtle.platform.services.workspace.services.WorkspaceService;
import com.syncturtle.testing.annotations.IntegrationTest;
import com.syncturtle.testing.annotations.UsePostgresDb;

@IntegrationTest
@UsePostgresDb("workspace_service_it")
class WorkspaceServiceIT {

    @Autowired
    WorkspaceService workspaceService;

    @Autowired
    WorkspaceRepository workspaceRepository;

    @Autowired
    WorkspaceMemberRepository workspaceMemberRepository;

}
