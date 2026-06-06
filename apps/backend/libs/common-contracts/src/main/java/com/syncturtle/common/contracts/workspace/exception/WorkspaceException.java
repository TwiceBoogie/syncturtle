package com.syncturtle.common.contracts.workspace.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.contracts.workspace.error.WorkspaceErrorCode;
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

    public static WorkspaceException of(WorkspaceErrorCode errorCode, Throwable cause) {
        return new WorkspaceException(errorCode, Map.of(), cause);
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
