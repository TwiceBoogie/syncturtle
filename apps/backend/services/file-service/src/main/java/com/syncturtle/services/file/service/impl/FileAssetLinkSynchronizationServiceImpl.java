package com.syncturtle.services.file.service.impl;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.file.FileAssetPurpose;
import com.syncturtle.common.contracts.file.exception.FileException;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;
import com.syncturtle.services.file.model.FileAsset;
import com.syncturtle.services.file.model.FileAssetLink;
import com.syncturtle.services.file.model.param.FileAssetLinkCreateParam;
import com.syncturtle.services.file.repository.FileAssetLinkRepository;
import com.syncturtle.services.file.repository.FileAssetRepository;
import com.syncturtle.services.file.service.FileAssetLinkSynchronizationService;
import com.syncturtle.services.file.service.collaborator.asset.FileAssetPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileAssetLinkSynchronizationServiceImpl implements FileAssetLinkSynchronizationService {

    private static final String WORKSPACE_SERVICE = "workspace-service";
    private static final String USER_SERVICE = "user-service";
    private static final String TARGET_WORKSPACE = "WORKSPACE";
    private static final String TARGET_USER = "USER";
    private static final String USAGE_WORKSPACE_LOGO = "WORKSPACE_LOGO";
    private static final String USAGE_USER_AVATAR = "USER_AVATAR";
    private static final String USAGE_USER_COVER = "USER_COVER";

    private final FileAssetRepository assetRepository;
    private final FileAssetLinkRepository linkRepository;
    private final FileAssetPolicy assetPolicy;
    private final Clock clock;

    @Override
    @Transactional
    public void synchronizeUserAssetLinks(UserEvent event) {
        Assert.notNull(event, "user event is required");

        UUID avatarAssetId = event.isDeleteEvent() ? null : event.getAvatarAssetId();
        UUID coverAssetId = event.isDeleteEvent() ? null : event.getCoverImageAssetId();

        Optional<FileAssetLink> avatarLink = findPrimarySlotForUpdate(
                USER_SERVICE,
                TARGET_USER,
                event.getId(),
                USAGE_USER_AVATAR);
        Optional<FileAssetLink> coverLink = findPrimarySlotForUpdate(
                USER_SERVICE,
                TARGET_USER,
                event.getId(),
                USAGE_USER_COVER);

        if (isStaleUserSnapshot(avatarLink, coverLink, event.getVersion())) {
            return;
        }

        synchronizeUserLink(
                avatarLink,
                event.getId(),
                avatarAssetId,
                FileAssetPurpose.USER_AVATAR,
                USAGE_USER_AVATAR,
                event.getVersion(),
                event.getUpdatedById());
        synchronizeUserLink(
                coverLink,
                event.getId(),
                coverAssetId,
                FileAssetPurpose.USER_COVER,
                USAGE_USER_COVER,
                event.getVersion(),
                event.getUpdatedById());
    }

    @Override
    @Transactional
    public void synchronizeWorkspaceAssetLinks(WorkspaceEvent event) {
        Assert.notNull(event, "workspace event is required");

        UUID logoAssetId = event.isDeleteEvent() ? null : event.getLogoAssetId();
        Optional<FileAssetLink> existing = findPrimarySlotForUpdate(
                WORKSPACE_SERVICE,
                TARGET_WORKSPACE,
                event.getId(),
                USAGE_WORKSPACE_LOGO);

        if (hasAlreadyProcessed(existing, event.getVersion(), logoAssetId)) {
            return;
        }

        if (logoAssetId == null) {
            removePrimaryLink(
                    existing,
                    event.getId(),
                    WORKSPACE_SERVICE,
                    TARGET_WORKSPACE,
                    event.getId(),
                    USAGE_WORKSPACE_LOGO,
                    event.getVersion());
            return;
        }

        FileAsset asset = requireActiveAsset(logoAssetId);
        assetPolicy.requireUploaded(asset);
        assetPolicy.requirePurpose(asset, FileAssetPurpose.WORKSPACE_LOGO);
        assetPolicy.requireWorkspace(asset, event.getId());

        savePrimaryLink(
                existing,
                asset,
                event.getId(),
                WORKSPACE_SERVICE,
                TARGET_WORKSPACE,
                event.getId(),
                USAGE_WORKSPACE_LOGO,
                event.getVersion(),
                event.getUpdatedById());
    }

    private Optional<FileAssetLink> findPrimarySlotForUpdate(
            String targetService,
            String targetType,
            UUID targetId,
            String usageType) {
        return linkRepository.findPrimarySlotForUpdate(targetService, targetType, targetId, usageType);
    }

    private static boolean isStaleUserSnapshot(
            Optional<FileAssetLink> avatarLink,
            Optional<FileAssetLink> coverLink,
            long sourceVersion) {
        long latestAppliedVersion = Long.MIN_VALUE;
        if (avatarLink.isPresent()) {
            latestAppliedVersion = Math.max(latestAppliedVersion, avatarLink.get().getSourceVersion());
        }
        if (coverLink.isPresent()) {
            latestAppliedVersion = Math.max(latestAppliedVersion, coverLink.get().getSourceVersion());
        }

        return sourceVersion < latestAppliedVersion;
    }

    private void synchronizeUserLink(
            Optional<FileAssetLink> existing,
            UUID userId,
            UUID assetId,
            FileAssetPurpose purpose,
            String usageType,
            long sourceVersion,
            UUID updatedByUserId) {
        if (hasAlreadyProcessed(existing, sourceVersion, assetId)) {
            return;
        }

        if (assetId == null) {
            removePrimaryLink(existing, null, USER_SERVICE, TARGET_USER, userId, usageType, sourceVersion);
            return;
        }

        FileAsset asset = requireActiveAsset(assetId);
        assetPolicy.requireUploaded(asset);
        assetPolicy.requirePurpose(asset, purpose);
        assetPolicy.requireOwner(asset, userId);

        savePrimaryLink(
                existing,
                asset,
                null,
                USER_SERVICE,
                TARGET_USER,
                userId,
                usageType,
                sourceVersion,
                updatedByUserId);
    }

    private static boolean hasAlreadyProcessed(Optional<FileAssetLink> existing, long sourceVersion,
            UUID expectedAssetId) {
        if (existing.isEmpty()) {
            return false;
        }

        FileAssetLink link = existing.get();
        long appliedVersion = link.getSourceVersion();
        if (sourceVersion < appliedVersion) {
            return true;
        }

        if (sourceVersion > appliedVersion) {
            return false;
        }

        boolean sameRemoval = expectedAssetId == null && link.isDeleted() && link.getAssetId() == null;
        boolean sameAsset = expectedAssetId != null && !link.isDeleted() && link.pointsTo(expectedAssetId);
        if (!sameRemoval && !sameAsset) {
            throw new IllegalStateException("Conflicting asset-link payload for equal source version. targetId="
                    + link.getTargetId() + " usageType=" + link.getUsageType() + " sourceVersion=" + sourceVersion);
        }

        return true;
    }

    private void savePrimaryLink(
            Optional<FileAssetLink> existing,
            FileAsset asset,
            UUID workspaceId,
            String targetService,
            String targetType,
            UUID targetId,
            String usageType,
            long sourceVersion,
            UUID linkedByUserId) {
        if (existing.isPresent()) {
            FileAssetLink link = existing.get();
            UUID previousAssetId = link.getAssetId();
            boolean changed = link.replaceAssetIfNewer(asset.getId(), linkedByUserId, sourceVersion);

            if (changed && !Objects.equals(previousAssetId, asset.getId())) {
                linkRepository.flush();
                deleteAssetWhenUnlinked(previousAssetId);
            }
            return;
        }

        FileAssetLink link = FileAssetLink.create(FileAssetLinkCreateParam.builder()
                .assetId(asset.getId())
                .workspaceId(workspaceId)
                .targetService(targetService)
                .targetType(targetType)
                .targetId(targetId)
                .usageType(usageType)
                .primary(true)
                .sourceVersion(sourceVersion)
                .linkedByUserId(linkedByUserId)
                .build());

        linkRepository.save(link);
    }

    private void removePrimaryLink(
            Optional<FileAssetLink> existing,
            UUID workspaceId,
            String targetService,
            String targetType,
            UUID targetId,
            String usageType,
            long sourceVersion) {
        if (existing.isPresent()) {
            FileAssetLink link = existing.get();
            UUID previousAssetId = link.getAssetId();
            boolean changed = link.markDeletedIfNewer(sourceVersion, clock);
            if (changed) {
                linkRepository.flush();
                deleteAssetWhenUnlinked(previousAssetId);
            }
            return;
        }

        FileAssetLink link = FileAssetLink.createDeletedPrimarySlot(FileAssetLinkCreateParam.builder()
                .workspaceId(workspaceId)
                .targetService(targetService)
                .targetType(targetType)
                .targetId(targetId)
                .usageType(usageType)
                .primary(true)
                .sourceVersion(sourceVersion)
                .build(), clock);

        linkRepository.save(link);
    }

    private FileAsset requireActiveAsset(UUID assetId) {
        return assetRepository.findByIdForUpdate(assetId)
                .filter(asset -> !asset.isDeleted())
                .orElseThrow(() -> FileException.assetNotFound(assetId));
    }

    private void deleteAssetWhenUnlinked(UUID assetId) {
        if (assetId == null || linkRepository.countByAssetIdAndDeletedAtIsNull(assetId) > 0) {
            return;
        }

        FileAsset asset = assetRepository.findByIdForUpdate(assetId).orElse(null);
        if (asset != null && !asset.isDeleted()) {
            asset.markDeleted(clock);
        }
    }

}
