package com.syncturtle.services.instance.models;

import org.hibernate.annotations.NaturalId;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.data.jpa.entity.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "instance_configurations")
public class InstanceConfiguration extends AuditedEntity {

    @NaturalId
    @Enumerated(EnumType.STRING)
    @Column(name = "key", nullable = false, unique = true)
    private InstanceConfigurationKey key;

    @Column(name = "value")
    private String value;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "is_encrypted", nullable = false)
    private boolean encrypted;
}
