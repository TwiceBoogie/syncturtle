package com.syncturtle.services.workspace.model;

import java.time.Clock;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.type.WorkspaceRole;
import com.syncturtle.common.data.jpa.entity.AuditedEntity;
import com.syncturtle.services.workspace.model.param.WorkspaceMemberCreateParam;
import com.syncturtle.services.workspace.model.support.WorkspaceRoleConverter;

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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "workspace_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMember extends AuditedEntity {

    @Convert(converter = WorkspaceRoleConverter.class)
    @Column(name = "role", nullable = false)
    private WorkspaceRole role;

    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_workspace_members_workspace"))
    private Workspace workspace;

    @Column(name = "company_role", columnDefinition = "TEXT")
    private String companyRole;

    @Column(name = "is_active", nullable = false)
    private boolean activated;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static WorkspaceMember create(WorkspaceMemberCreateParam param) {
        Assert.notNull(param, "workspace member create param is required");

        WorkspaceMember member = new WorkspaceMember();
        member.initializeForCreate(param);
        return member;
    }

    public UUID getWorkspaceId() {
        return workspace.getId();
    }

    public boolean belongsToWorkspace(UUID workspaceId) {
        Assert.notNull(workspaceId, "workspaceId is required");

        return workspaceId.equals(getWorkspaceId());
    }

    public boolean belongsToMember(UUID memberId) {
        Assert.notNull(memberId, "memberId is required");

        return memberId.equals(this.memberId);
    }

    public void changeRole(WorkspaceRole newRole) {
        requireActive("WorkspaceMember");
        Assert.notNull(newRole, "newRole is required");

        if (role == newRole) {
            return;
        }

        role = newRole;
    }

    public void updateCompanyRole(String companyRole) {
        requireActive("WorkspaceMember");

        this.companyRole = normalizeNullable(companyRole);
    }

    public void activate() {
        requireActive("WorkspaceMember");

        if (activated) {
            return;
        }

        activated = true;
    }

    public void deactivate() {
        requireActive("WorkspaceMember");

        if (!activated) {
            return;
        }

        activated = false;
    }

    public void revoke(Clock clock) {
        requireActive("WorkspaceMember");

        activated = false;
        softDelete(clock);
    }

    private void initializeForCreate(WorkspaceMemberCreateParam param) {
        this.role = param.getRole();
        this.memberId = param.getMemberId();
        this.workspace = param.getWorkspace();
        this.companyRole = param.getCompanyRole();
        this.activated = true;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
