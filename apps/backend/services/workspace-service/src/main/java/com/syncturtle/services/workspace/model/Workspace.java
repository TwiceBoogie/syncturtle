package com.syncturtle.services.workspace.model;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.common.data.jpa.entity.AuditedEntity;
import com.syncturtle.services.workspace.model.param.WorkspaceCreateParam;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "workspaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends AuditedEntity {

    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_SLUG_LENGTH = 48;
    private static final int MAX_ORGANIZATION_SIZE_LENGTH = 20;
    private static final int MAX_TIMEZONE_LENGTH = 255;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "logo_asset_id")
    private UUID logoAssetId;

    @Column(name = "slug", nullable = false, length = MAX_SLUG_LENGTH)
    private String slug;

    @Column(name = "organization_size", length = MAX_ORGANIZATION_SIZE_LENGTH)
    private String organizationSize;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "timezone", nullable = false, length = MAX_TIMEZONE_LENGTH)
    private String timezone;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static Workspace create(WorkspaceCreateParam param) {
        Assert.notNull(param, "workspace create param is required");

        Workspace workspace = new Workspace();
        workspace.initializeForCreate(param);
        return workspace;
    }

    public void rename(String name) {
        requireActive("Workspace");

        this.name = normalizeRequired(name, "name", MAX_NAME_LENGTH);
    }

    public void changeSlug(String slug) {
        requireActive("Workspace");

        this.slug = normalizeRequired(slug, "slug", MAX_SLUG_LENGTH);
    }

    public void updateOrganizationSize(String organizationSize) {
        requireActive("Workspace");

        this.organizationSize = normalizeRequired(organizationSize, "organizationSize", MAX_ORGANIZATION_SIZE_LENGTH);
    }

    public void updateTimezone(String timezone) {
        requireActive("Workspace");

        this.timezone = normalizeTimezone(timezone);
    }

    public void transferOwnership(UUID ownerId) {
        requireActive("Workspace");
        Assert.notNull(ownerId, "ownerId is required");

        if (ownerId.equals(this.ownerId)) {
            return;
        }

        this.ownerId = ownerId;
    }

    public void updateLogo(UUID logoAssetId) {
        requireActive("Workspace");

        this.logoAssetId = logoAssetId;
    }

    public void clearLogo() {
        requireActive("Workspace");

        this.logoAssetId = null;
    }

    public void delete(Clock clock) {
        requireActive("Workspace");

        softDelete(clock);
    }

    public void restoreWorkspace() {
        restore();
    }

    private void initializeForCreate(WorkspaceCreateParam param) {
        this.name = normalizeRequired(param.getName(), "name", MAX_NAME_LENGTH);
        this.slug = normalizeRequired(param.getSlug(), "slug", MAX_SLUG_LENGTH);
        this.organizationSize = normalizeRequired(param.getOrganizationSize(), "organizationSize",
                MAX_ORGANIZATION_SIZE_LENGTH);
        this.ownerId = param.getOwnerId();
        this.timezone = normalizeTimezone(param.getTimezone());
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

    private static String normalizeTimezone(String value) {
        String normalized = normalizeRequired(value, "timezone", MAX_TIMEZONE_LENGTH);

        try {
            ZoneId.of(normalized);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("timezone must be a valid IANA time zone", exception);
        }

        return normalized;
    }

}
