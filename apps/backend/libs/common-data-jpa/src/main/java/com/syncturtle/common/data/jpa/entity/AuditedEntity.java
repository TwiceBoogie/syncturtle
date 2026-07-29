package com.syncturtle.common.data.jpa.entity;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by_id", updatable = false)
    private UUID createdById;

    @LastModifiedBy
    @Column(name = "updated_by_id")
    private UUID updatedById;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public final boolean isDeleted() {
        return deletedAt != null;
    }

    public final boolean isActive() {
        return deletedAt == null;
    }

    protected final void softDelete(Clock clock) {
        Assert.notNull(clock, "clock is required");

        if (isDeleted()) {
            return;
        }

        deletedAt = Instant.now(clock);
    }

    protected final void softDelete(Instant deletedAt) {
        Assert.notNull(
                deletedAt,
                "deletedAt is required");

        if (isDeleted()) {
            return;
        }

        this.deletedAt = deletedAt;
    }

    protected final void restore() {
        deletedAt = null;
    }

    protected final void requireActive(
            String aggregateName) {
        Assert.hasText(
                aggregateName,
                "aggregateName is required");

        Assert.state(
                isActive(),
                aggregateName + " is deleted");
    }

    protected final void requirePersisted(
            String aggregateName) {
        Assert.hasText(
                aggregateName,
                "aggregateName is required");

        Assert.state(
                id != null,
                aggregateName + " must be persisted");
    }
}