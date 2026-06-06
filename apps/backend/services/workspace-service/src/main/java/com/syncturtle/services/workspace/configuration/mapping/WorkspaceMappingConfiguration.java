package com.syncturtle.services.workspace.configuration.mapping;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.syncturtle.common.web.pagination.CursorCodec;
import com.syncturtle.services.workspace.service.mapper.WorkspacePageMapper;
import com.syncturtle.services.workspace.service.mapper.WorkspaceResponseMapper;

@Configuration(proxyBeanMethods = false)
public class WorkspaceMappingConfiguration {

    @Bean
    WorkspacePageMapper workspacePageMapper(
            CursorCodec cursorCodec,
            WorkspaceResponseMapper workspaceResponseMapper) {
        return new WorkspacePageMapper(cursorCodec, workspaceResponseMapper);
    }

}
