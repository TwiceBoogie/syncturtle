package com.syncturtle.platform.services.workspace.models;

import java.time.Instant;

import com.syncturtle.common.data.jpa.model.AuditedEntity;
import com.syncturtle.platform.services.workspace.enums.WorkspaceRole;
import com.syncturtle.platform.services.workspace.models.support.WorkspaceRoleConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "workspace_member_invites")
public class WorkspaceMemberInvite extends AuditedEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "accepted", nullable = false)
    private boolean accepted;

    @Column(name = "token", nullable = false, length = 255)
    private String token;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Convert(converter = WorkspaceRoleConverter.class)
    @Column(name = "role", nullable = false)
    private WorkspaceRole role = WorkspaceRole.GUEST;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

}
