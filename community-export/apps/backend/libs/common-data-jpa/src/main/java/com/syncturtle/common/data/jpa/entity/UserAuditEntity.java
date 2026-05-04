package com.syncturtle.common.data.jpa.entity;

import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class UserAuditEntity {

    @CreatedBy
    @Column(name = "created_by_id")
    private UUID createdById;

    @LastModifiedBy
    @Column(name = "updated_by_id")
    private UUID updatedById;

}
