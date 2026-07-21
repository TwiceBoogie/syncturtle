package com.syncturtle.services.instance.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.web.pagination.CursorCodec;
import com.syncturtle.common.web.pagination.CursorPageResponse;
import com.syncturtle.common.web.pagination.CursorPosition;
import com.syncturtle.common.web.pagination.DecodedCursor;
import com.syncturtle.services.instance.client.WorkspaceClient;
import com.syncturtle.services.instance.dto.request.InstanceWorkspaceCreateRequest;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceResponse;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceSlugCheckResponse;
import com.syncturtle.services.instance.repository.InstanceRepository;
import com.syncturtle.services.instance.repository.WorkspaceRepository;
import com.syncturtle.services.instance.repository.projection.InstanceIdProjection;
import com.syncturtle.services.instance.repository.projection.InstanceWorkspaceProjection;
import com.syncturtle.services.instance.service.InstanceWorkspaceAdminService;
import com.syncturtle.services.instance.service.mapper.InstanceWorkspacePageMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceWorkspaceAdminServiceImpl implements InstanceWorkspaceAdminService {

    private static final int MAX_PER_PAGE = 100;

    private final InstanceRepository instanceRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceClient workspaceClient;
    private final CursorCodec cursorCodec;
    private final InstanceWorkspacePageMapper pageMapper;

    @Override
    @Transactional(readOnly = true)
    public InstanceWorkspaceSlugCheckResponse checkSlug(UUID currentUserId, String slug) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.hasText(slug, "slug is required");

        UUID instanceId = requireCurrentInstanceId();

        return workspaceClient.checkSlug(instanceId, slug);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<InstanceWorkspaceResponse> getWorkspaces(UUID currentUserId, String cursor, int perPage,
            String search) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.isTrue(perPage > 0, "perPage must be greater than 0");
        Assert.isTrue(perPage <= MAX_PER_PAGE, "perPage must be less than or equal to " + MAX_PER_PAGE);

        requireCurrentInstanceId();

        // 1: decode cursor which contains (ID, createdAt)
        DecodedCursor decoded = cursorCodec.decode(cursor);

        CursorPosition cursorPosition = decoded == null
                ? null
                : decoded.toPosition();

        // 2: build pattern to avoid db side concatenation
        String pattern = toSearchPattern(search);

        List<InstanceWorkspaceProjection> workspaces = workspaceRepository.findWorkspacePageDesc(
                pattern,
                cursorPosition == null ? null : cursorPosition.getCreatedAt(),
                cursorPosition == null ? null : cursorPosition.getId(),
                perPage + 1);

        return pageMapper.mapToPageResponse(workspaces, perPage);
    }

    @Override
    @Transactional
    public InstanceWorkspaceResponse createWorkspace(UUID currentUserId, InstanceWorkspaceCreateRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(request, "workspace create request is required");

        UUID instanceId = requireCurrentInstanceId();

        return workspaceClient.createWorkspace(instanceId, request);
    }

    private UUID requireCurrentInstanceId() {
        return instanceRepository
                .findFirstByDeletedAtIsNullOrderByCreatedAtDesc(InstanceIdProjection.class)
                .map(InstanceIdProjection::getId)
                .orElseThrow(() -> new IllegalStateException("Instance has not been initialized."));
    }

    private static String toSearchPattern(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }

        String normalized = search.trim().toLowerCase();

        return "%" + normalized + "%";
    }

}
