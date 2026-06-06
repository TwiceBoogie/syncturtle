package com.syncturtle.services.instance.model;

import java.time.Clock;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.data.jpa.entity.AuditedEntity;
import com.syncturtle.services.instance.model.support.InstanceRoleConverter;
import com.syncturtle.services.instance.type.InstanceAdminRole;

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
@Table(name = "instance_admins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceAdmin extends AuditedEntity {

    private static final Long INITIAL_SESSION_VERSION = 1L;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_instance_admins_instance"))
    private Instance instance;

    @Convert(converter = InstanceRoleConverter.class)
    @Column(name = "role", nullable = false)
    private InstanceAdminRole role;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "session_version", nullable = false)
    private Long sessionVersion;

    private InstanceAdmin(UUID userId, Instance instance, InstanceAdminRole role, boolean verified) {
        Assert.notNull(userId, "userId is required");
        Assert.notNull(instance, "instance is required");
        Assert.notNull(instance.getId(), "instance must be persisted before assigning an admin");
        Assert.notNull(role, "role is required");

        this.userId = userId;
        this.instance = instance;
        this.role = role;
        this.verified = verified;
        this.sessionVersion = INITIAL_SESSION_VERSION;
    }

    public static InstanceAdmin createAdmin(UUID userId, Instance instance) {
        return new InstanceAdmin(userId, instance, InstanceAdminRole.ADMIN, false);
    }

    public static InstanceAdmin createInitialOwner(UUID userId, Instance instance) {
        return new InstanceAdmin(userId, instance, InstanceAdminRole.ADMIN, false);
    }

    public UUID getInstanceId() {
        return instance.getId();
    }

    public boolean isAdmin() {
        return role == InstanceAdminRole.ADMIN;
    }

    public boolean hasAtLeastRole(InstanceAdminRole requiredRole) {
        Assert.notNull(requiredRole, "required is required");
        return role.isAtLeast(requiredRole);
    }

    public void verify() {
        requireActive("InstanceAdmin");

        if (verified) {
            return;
        }

        verified = true;
        bumpSessionVersion();
    }

    public void unverify() {
        requireActive("InstanceAdmin");

        if (!verified) {
            return;
        }

        verified = false;
        bumpSessionVersion();
    }

    public void changeRole(InstanceAdminRole newRole) {
        requireActive("InstanceAdmin");
        Assert.notNull(newRole, "newRole is required");

        if (role == newRole) {
            return;
        }

        role = newRole;
        bumpSessionVersion();
    }

    public void revoke(Clock clock) {
        requireActive("InstanceAdmin");
        softDelete(clock);
        bumpSessionVersion();
    }

    public void bumpSessionVersion() {
        requireActive("InstanceAdmin");

        if (sessionVersion == Long.MAX_VALUE) {
            throw new IllegalStateException("sessionVersion overflow");
        }

        this.sessionVersion++;
    }
}
