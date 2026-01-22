package com.syncturtle.common.data.jpa.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class SoftDeleteEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected void markDeleted(Instant now) {
        this.deletedAt = now;
    }

    protected boolean isDeleted() {
        return deletedAt != null;
    }

}
