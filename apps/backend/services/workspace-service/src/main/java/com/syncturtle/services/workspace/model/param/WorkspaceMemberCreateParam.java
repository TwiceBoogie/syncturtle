package com.syncturtle.services.workspace.model.param;

import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.services.workspace.model.Workspace;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class WorkspaceMemberCreateParam {

    private final WorkspaceRole role;
    private final UUID memberId;
    private final Workspace workspace;
    private final String companyRole;

    @Builder
    private WorkspaceMemberCreateParam(
            WorkspaceRole role,
            UUID memberId,
            Workspace workspace,
            String companyRole) {
        Assert.notNull(memberId, "memberId is required");
        Assert.notNull(workspace, "workspace is required");
        Assert.notNull(workspace.getId(), "workspace must be persisted before assigning members");

        this.role = role == null ? WorkspaceRole.GUEST : role;
        this.memberId = memberId;
        this.workspace = workspace;
        this.companyRole = normalizeNullable(companyRole);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
