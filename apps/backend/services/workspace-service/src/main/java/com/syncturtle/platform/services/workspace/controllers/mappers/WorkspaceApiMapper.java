package com.syncturtle.platform.services.workspace.controllers.mappers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.syncturtle.common.spring.mapping.BasicMapper;
import com.syncturtle.common.web.dto.response.CursorPageResponse;
import com.syncturtle.platform.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.platform.services.workspace.repositories.projections.WorkspaceProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class WorkspaceApiMapper {

    private final BasicMapper basicMapper;

    public CursorPageResponse<WorkspaceResponse> toCursorPageWorkspaceResponse(List<WorkspaceProjection> workspaces,
            int perPage) {
        return basicMapper.convertToCursorPageResponse(workspaces, perPage, WorkspaceResponse.class);
    }

    public WorkspaceResponse toWorkspaceResponse(WorkspaceProjection projection) {
        return basicMapper.convertToResponse(projection, WorkspaceResponse.class);
    }

}
