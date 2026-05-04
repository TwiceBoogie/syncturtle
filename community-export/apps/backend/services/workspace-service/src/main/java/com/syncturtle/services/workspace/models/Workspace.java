package com.syncturtle.services.workspace.models;

import java.util.Objects;
import java.util.UUID;

import com.syncturtle.common.data.jpa.entity.AuditedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "workspaces")
public class Workspace extends AuditedEntity {

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "logo", columnDefinition = "TEXT")
    private String logo;

    @Column(name = "logo_asset_id")
    private UUID logoAssetId;

    @Column(name = "slug", nullable = false, length = 48)
    private String slug;

    @Column(name = "organization_size", length = 20)
    private String organizationSize;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    @Column(name = "timezone", nullable = false, length = 255)
    private String timezone;

    @Version
    private Long version;

    public static Workspace create(String name, String slug, String organizationSize, UUID ownerId) {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(slug, "slug is required");
        Objects.requireNonNull(organizationSize, "organizationSize is required");
        Objects.requireNonNull(ownerId, "ownerId is required");

        Workspace workspace = new Workspace();
        workspace.name = name;
        workspace.slug = slug;
        workspace.organizationSize = organizationSize;
        workspace.ownerId = ownerId;
        workspace.timezone = "America/Chicago";

        return workspace;
    }

}
