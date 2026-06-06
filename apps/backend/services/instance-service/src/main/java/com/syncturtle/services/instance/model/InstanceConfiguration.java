package com.syncturtle.services.instance.model;

import java.util.Objects;

import org.hibernate.annotations.NaturalId;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.data.jpa.entity.AuditedEntity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "instance_configurations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceConfiguration extends AuditedEntity {

    @NaturalId
    @Enumerated(EnumType.STRING)
    @Column(name = "key", nullable = false, unique = true, updatable = false)
    private InstanceConfigurationKey key;

    @Column(name = "value")
    private String value;

    @Column(name = "category", nullable = false, updatable = false)
    private String category;

    @Column(name = "is_encrypted", nullable = false, updatable = false)
    private boolean encrypted;

    private InstanceConfiguration(InstanceConfigurationKey key, String value, String category, boolean encrypted) {
        Assert.notNull(key, "configuration key is required");
        Assert.hasText(category, "configuration category is required");

        this.key = key;
        this.value = value;
        this.category = category.trim();
        this.encrypted = encrypted;
    }

    public static InstanceConfiguration create(InstanceConfigurationKey key, String storedValue, String category,
            boolean encrypted) {
        return new InstanceConfiguration(key, storedValue, category, encrypted);
    }

    public boolean replaceStoredValue(String nextStoredValue) {
        requireActive("InstanceConfiguration");

        if (Objects.equals(value, nextStoredValue)) {
            return false;
        }

        value = nextStoredValue;
        return true;
    }
}
