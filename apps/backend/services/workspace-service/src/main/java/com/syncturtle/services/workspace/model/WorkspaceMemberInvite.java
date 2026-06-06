package com.syncturtle.services.workspace.model;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.data.jpa.entity.AuditedEntity;
import com.syncturtle.services.workspace.model.param.WorkspaceMemberInviteCreateParam;
import com.syncturtle.services.workspace.model.support.WorkspaceRoleConverter;
import com.syncturtle.services.workspace.type.WorkspaceRole;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "workspace_member_invites")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMemberInvite extends AuditedEntity {

    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_TOKEN_LENGTH = 255;

    @Column(name = "email", nullable = false, length = MAX_EMAIL_LENGTH)
    private String email;

    @Column(name = "accepted", nullable = false)
    private boolean accepted;

    @Column(name = "token", nullable = false, length = MAX_TOKEN_LENGTH)
    private String token;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Convert(converter = WorkspaceRoleConverter.class)
    @Column(name = "role", nullable = false)
    private WorkspaceRole role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_workspace_member_invites_workspace"))
    private Workspace workspace;

    public static WorkspaceMemberInvite create(WorkspaceMemberInviteCreateParam param) {
        Assert.notNull(param, "workspace member invite create param is required");

        WorkspaceMemberInvite invite = new WorkspaceMemberInvite();
        invite.initializeForCreate(param);
        return invite;
    }

    public boolean isPending() {
        return isActive() && respondedAt == null;
    }

    public boolean isDeclined() {
        return respondedAt != null && !accepted;
    }

    public UUID getWorkspaceId() {
        return workspace.getId();
    }

    public void accept(Clock clock) {
        requireActive("WorkspaceMemberInvite");
        Assert.notNull(clock, "clock is required");
        requirePendingResponse();

        accepted = true;
        respondedAt = Instant.now(clock);
    }

    public void decline(Clock clock) {
        requireActive("WorkspaceMemberInvite");
        Assert.notNull(clock, "clock is required");
        requirePendingResponse();

        accepted = false;
        respondedAt = Instant.now(clock);
    }

    public void changeRole(WorkspaceRole newRole) {
        requireActive("WorkspaceMemberInvite");
        requirePendingResponse();
        Assert.notNull(newRole, "newRole is required");

        if (role == newRole) {
            return;
        }

        role = newRole;
    }

    public void updateMessage(String message) {
        requireActive("WorkspaceMemberInvite");
        requirePendingResponse();

        this.message = normalizeNullable(message);
    }

    public void rotateToken(String token) {
        requireActive("WorkspaceMemberInvite");
        requirePendingResponse();

        this.token = normalizeRequired(token, "token", MAX_TOKEN_LENGTH);
    }

    public void revoke(Clock clock) {
        requireActive("WorkspaceMemberInvite");

        softDelete(clock);
    }

    private void initializeForCreate(WorkspaceMemberInviteCreateParam param) {
        this.email = normalizeRequired(param.getEmail(), "email", MAX_EMAIL_LENGTH);
        this.token = normalizeRequired(param.getToken(), "token", MAX_TOKEN_LENGTH);
        this.message = normalizeNullable(param.getMessage());
        this.role = param.getRole();
        this.workspace = param.getWorkspace();
        this.accepted = false;
        this.respondedAt = null;
    }

    private void requirePendingResponse() {
        Assert.state(respondedAt == null, "workspace member invite has already been responded to");
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
