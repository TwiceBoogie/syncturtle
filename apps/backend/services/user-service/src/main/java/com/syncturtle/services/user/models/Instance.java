package com.syncturtle.services.user.models;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.contracts.instance.model.InstanceEdition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "instances_lite")
public class Instance {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "edition", nullable = false, length = 50)
    private InstanceEdition edition;

    @Column(name = "is_setup_done", nullable = false)
    private boolean setupDone;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "version", nullable = false)
    private Long version;

}
