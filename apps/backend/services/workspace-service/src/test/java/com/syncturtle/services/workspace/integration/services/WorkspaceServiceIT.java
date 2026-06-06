package com.syncturtle.services.workspace.integration.services;

import org.springframework.beans.factory.annotation.Autowired;

import com.syncturtle.services.workspace.repository.WorkspaceMemberRepository;
import com.syncturtle.services.workspace.repository.WorkspaceRepository;
import com.syncturtle.services.workspace.service.WorkspaceService;
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
