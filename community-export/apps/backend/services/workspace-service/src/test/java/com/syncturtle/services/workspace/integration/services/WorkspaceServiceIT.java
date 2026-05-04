package com.syncturtle.services.workspace.integration.services;

import org.springframework.beans.factory.annotation.Autowired;

import com.syncturtle.services.workspace.repositories.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.repositories.WorkspaceRepository;
import com.syncturtle.services.workspace.services.WorkspaceService;
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
