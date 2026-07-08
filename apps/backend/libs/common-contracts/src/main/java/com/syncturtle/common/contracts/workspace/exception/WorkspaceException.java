package com.syncturtle.common.contracts.workspace.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.syncturtle.common.contracts.workspace.error.WorkspaceErrorCode;
import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.common.core.exceptions.SyncturtleServiceException;

public final class WorkspaceException extends SyncturtleServiceException {

    private final WorkspaceErrorCode workspaceErrorCode;

    private WorkspaceException(WorkspaceErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(errorCode, payload, cause);
        this.workspaceErrorCode = Objects.requireNonNull(errorCode, "errorCode is required");
    }

    public WorkspaceErrorCode getWorkspaceErrorCode() {
        return workspaceErrorCode;
    }

    public static WorkspaceException of(WorkspaceErrorCode errorCode) {
        return new WorkspaceException(errorCode, Map.of(), null);
    }

    public static WorkspaceException of(WorkspaceErrorCode errorCode, Map<String, Object> payload) {
        return new WorkspaceException(errorCode, payload, null);
    }

    public static WorkspaceException of(WorkspaceErrorCode errorCode, Throwable cause) {
        return new WorkspaceException(errorCode, Map.of(), cause);
    }

    public static WorkspaceException notFound(String workspaceSlug) {
        return new WorkspaceException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND, Map.of("workspace_slug", workspaceSlug),
                null);
    }

    public static WorkspaceException slugAlreadyExists(String workspaceSlug) {
        return new WorkspaceException(WorkspaceErrorCode.WORKSPACE_SLUG_ALREADY_EXISTS,
                Map.of("workspace_slug", workspaceSlug), null);
    }

    public static WorkspaceException logoAssetInvalid(UUID assetId) {
        return new WorkspaceException(WorkspaceErrorCode.WORKSPACE_LOGO_ASSET_INVALID, Map.of("asset_id", assetId),
                null);
    }

    public static WorkspaceException logoAssetNotUploaded(UUID assetId) {
        return new WorkspaceException(WorkspaceErrorCode.WORKSPACE_LOGO_ASSET_NOT_UPLOADED, Map.of("asset_id", assetId),
                null);
    }

    public static WorkspaceException logoAssetForbidden(UUID assetId, UUID workspaceId) {
        return new WorkspaceException(WorkspaceErrorCode.WORKSPACE_LOGO_ASSET_FORBIDDEN,
                Map.of("asset_id", assetId, "workspace_id", workspaceId), null);
    }

    public static WorkspaceException invitationForbidden(UUID workspaceId, UUID currentUserId) {
        return new WorkspaceException(WorkspaceErrorCode.WORKSPACE_INVITATION_FORBIDDEN,
                Map.of("workspace_id", workspaceId, "user_id", currentUserId), null);
    }

    public static WorkspaceException invitationRoleTooHigh(WorkspaceRole requesterRole, WorkspaceRole requestedRole) {
        return new WorkspaceException(WorkspaceErrorCode.WORKSPACE_INVITATION_ROLE_TOO_HIGH,
                Map.of("requestor_role", requesterRole, "requested_role", requestedRole), null);
    }

    public static WorkspaceException invitationUsersAlreadyMember(UUID workspaceId, List<String> emails) {
        return new WorkspaceException(WorkspaceErrorCode.WORKSPACE_INVITATION_USER_ALREADY_MEMBER,
                Map.of("workspace_id", workspaceId, "emails", List.copyOf(emails)), null);
    }

    public static WorkspaceException invitationCreateFailed(Throwable cause) {
        return new WorkspaceException(WorkspaceErrorCode.WORKSPACE_INVITATION_CREATE_FAILED, Map.of(), cause);
    }

    public static WorkspaceException duplicateInvitationEmail(String email) {
        return new WorkspaceException(WorkspaceErrorCode.WORKSPACE_INVITATION_DUPLICATE_EMAIL, Map.of("email", email),
                null);
    }

    public WorkspaceException with(String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(getPayload());

        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }

        return new WorkspaceException(workspaceErrorCode, copy, getCause());
    }

}
