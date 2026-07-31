package com.syncturtle.services.workspace.model.param;

import java.util.Locale;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.services.workspace.model.Workspace;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class WorkspaceMemberInviteCreateParam {

    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_TOKEN_LENGTH = 255;

    private final String email;
    private final String token;
    private final String message;
    private final WorkspaceRole role;
    private final Workspace workspace;

    @Builder
    private WorkspaceMemberInviteCreateParam(
            String email,
            String token,
            String message,
            WorkspaceRole role,
            Workspace workspace) {
        Assert.notNull(workspace, "workspace is required");
        Assert.notNull(workspace.getId(), "workspace must be persisted before creating invites");

        this.email = normalizeEmail(email);
        this.token = normalizeRequired(token, "token", MAX_TOKEN_LENGTH);
        this.message = normalizeNullable(message);
        this.role = role == null ? WorkspaceRole.GUEST : role;
        this.workspace = workspace;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeRequired(value, "email", MAX_EMAIL_LENGTH);
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
