package com.syncturtle.services.workspace.models;

import java.util.UUID;

import com.syncturtle.common.data.jpa.entity.AuditedEntity;
import com.syncturtle.services.workspace.enums.WorkspaceRole;
import com.syncturtle.services.workspace.models.support.WorkspaceRoleConverter;

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
@Table(name = "workspace_members")
public class WorkspaceMember extends AuditedEntity {

    @Convert(converter = WorkspaceRoleConverter.class)
    @Column(name = "role", nullable = false)
    private WorkspaceRole role = WorkspaceRole.GUEST;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "company_role", columnDefinition = "TEXT")
    private String companyRole;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public static WorkspaceMember create(WorkspaceRole role, UUID memberId, Workspace workspace, String companyRole) {
        WorkspaceMember workspaceMember = new WorkspaceMember();
        workspaceMember.role = role;
        workspaceMember.memberId = memberId;
        workspaceMember.workspace = workspace;
        workspaceMember.companyRole = companyRole;
        workspaceMember.active = true;

        return workspaceMember;
    }

}
