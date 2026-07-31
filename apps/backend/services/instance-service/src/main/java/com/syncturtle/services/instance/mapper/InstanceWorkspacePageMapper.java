package com.syncturtle.services.instance.mapper;

import org.springframework.util.Assert;

import com.syncturtle.common.web.pagination.CursorCodec;
import com.syncturtle.common.web.pagination.mapper.AbstractCursorPageMapper;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceResponse;
import com.syncturtle.services.instance.repository.projection.InstanceWorkspaceProjection;

public final class InstanceWorkspacePageMapper
        extends AbstractCursorPageMapper<InstanceWorkspaceProjection, InstanceWorkspaceResponse> {

    private final InstanceWorkspaceResponseMapper mapper;

    public InstanceWorkspacePageMapper(CursorCodec cursorCodec, InstanceWorkspaceResponseMapper mapper) {
        super(cursorCodec);

        Assert.notNull(mapper, "mapper is required");

        this.mapper = mapper;
    }

    @Override
    protected InstanceWorkspaceResponse mapToResponse(InstanceWorkspaceProjection source) {
        return mapper.toResponse(source);
    }

}
