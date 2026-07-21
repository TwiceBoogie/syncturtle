package com.syncturtle.services.workspace.service.mapper;

import org.springframework.util.Assert;

import com.syncturtle.common.web.pagination.CursorCodec;
import com.syncturtle.common.web.pagination.mapper.AbstractCursorPageMapper;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.repository.projection.CurrentUserWorkspaceProjection;

public final class WorkspacePageMapper
        extends AbstractCursorPageMapper<CurrentUserWorkspaceProjection, WorkspaceResponse> {

    private final WorkspaceResponseMapper workspaceResponseMapper;

    public WorkspacePageMapper(CursorCodec cursorCodec, WorkspaceResponseMapper workspaceResponseMapper) {
        super(cursorCodec);

        Assert.notNull(workspaceResponseMapper, "workspaceResponseMapper is required");
        this.workspaceResponseMapper = workspaceResponseMapper;
    }

    @Override
    protected WorkspaceResponse mapToResponse(CurrentUserWorkspaceProjection source) {
        return workspaceResponseMapper.toResponse(source);
    }

}
