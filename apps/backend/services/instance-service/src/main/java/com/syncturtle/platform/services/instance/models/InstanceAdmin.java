package com.syncturtle.platform.services.instance.models;

import java.util.UUID;

import com.syncturtle.common.data.jpa.model.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "instance_admins")
public class InstanceAdmin extends AuditedEntity {

    @Column(name = "role", nullable = false)
    private int role;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "user_id")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private Instance instance;
}
