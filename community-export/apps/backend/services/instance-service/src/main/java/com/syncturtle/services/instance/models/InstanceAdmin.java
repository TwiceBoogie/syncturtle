package com.syncturtle.services.instance.models;

import java.util.Objects;
import java.util.UUID;

import com.syncturtle.common.data.jpa.entity.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "instance_admins")
public class InstanceAdmin extends AuditedEntity {

    @Column(name = "role", nullable = false)
    private Integer role;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "user_id")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private Instance instance;

    @Column(name = "session_version", nullable = false)
    private Long sessionVersion = 1L;

    public UUID getInstanceId() {
        return instance.getId();
    }

    public void bumpSessionVersion() {
        sessionVersion = (sessionVersion == null ? 1L : sessionVersion + 1L);
    }

    public static InstanceAdmin create(UUID userId, Instance instance) {
        Objects.requireNonNull(userId, "User model");
        Objects.requireNonNull(instance, "Instance model");
        InstanceAdmin admin = new InstanceAdmin();
        admin.userId = userId;
        admin.instance = instance;
        return admin;
    }

    @PrePersist
    void prePersist() {
        if (role == null) {
            role = 20;
        }
    }
}
