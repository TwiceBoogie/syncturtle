package com.syncturtle.services.file.service.impl;

import java.time.Clock;
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
import com.syncturtle.services.file.service.FileAssetLinkService;
import com.syncturtle.services.file.service.asset.FileAssetPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileAssetLinkServiceImpl implements FileAssetLinkService {

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
    public void upsertPrimaryWorkspaceLogoLink(WorkspaceEvent event) {
        Assert.notNull(event, "workspace event is required");

        UUID logoAssetId = event.getLogoAssetId();

        if (logoAssetId == null) {
            removePrimaryWorkspaceLogoLink(event.getId(), event.getVersion());
            return;
        }

        Optional<FileAssetLink> existing = findPrimarySlotForUpdate(WORKSPACE_SERVICE, TARGET_WORKSPACE, event.getId(),
                USAGE_WORKSPACE_LOGO);

        if (hasAlreadyProcessed(existing, event.getVersion())) {
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

    @Override
    @Transactional
    public void removePrimaryWorkspaceLogoLink(UUID workspaceId, Long sourceVersion) {
        removePrimaryLinkIfNewer(
                workspaceId,
                WORKSPACE_SERVICE,
                TARGET_WORKSPACE,
                workspaceId,
                USAGE_WORKSPACE_LOGO,
                sourceVersion);
    }

    @Override
    @Transactional
    public void upsertPrimaryUserAvatarLink(UserEvent event) {
        Assert.notNull(event, "user event is required");

        UUID avatarAssetId = event.getAvatarAssetId();

        if (avatarAssetId == null) {
            removePrimaryUserAvatarLink(event.getId(), event.getVersion());
            return;
        }

        Optional<FileAssetLink> existing = findPrimarySlotForUpdate(USER_SERVICE, TARGET_USER, event.getId(),
                USAGE_USER_AVATAR);

        if (hasAlreadyProcessed(existing, event.getVersion())) {
            return;
        }

        FileAsset asset = requireActiveAsset(avatarAssetId);

        assetPolicy.requireUploaded(asset);
        assetPolicy.requirePurpose(asset, FileAssetPurpose.USER_AVATAR);
        assetPolicy.requireOwner(asset, event.getId());

        savePrimaryLink(
                existing,
                asset,
                null,
                USER_SERVICE,
                TARGET_USER,
                event.getId(),
                USAGE_USER_AVATAR,
                event.getVersion(),
                event.getUpdatedById());
    }

    @Override
    @Transactional
    public void removePrimaryUserAvatarLink(UUID userId, Long sourceVersion) {
        removePrimaryLinkIfNewer(
                null,
                USER_SERVICE,
                TARGET_USER,
                userId,
                USAGE_USER_AVATAR,
                sourceVersion);
    }

    @Override
    @Transactional
    public void upsertPrimaryUserCoverLink(UserEvent event) {
        Assert.notNull(event, "user event is required");

        UUID coverImageAssetId = event.getCoverImageAssetId();

        if (coverImageAssetId == null) {
            removePrimaryUserCoverLink(event.getId(), event.getVersion());
            return;
        }

        Optional<FileAssetLink> existing = findPrimarySlotForUpdate(USER_SERVICE, TARGET_USER, event.getId(),
                USAGE_USER_COVER);

        if (hasAlreadyProcessed(existing, event.getVersion())) {
            return;
        }

        FileAsset asset = requireActiveAsset(coverImageAssetId);

        assetPolicy.requireUploaded(asset);
        assetPolicy.requirePurpose(asset, FileAssetPurpose.USER_COVER);
        assetPolicy.requireOwner(asset, event.getId());

        savePrimaryLink(
                existing,
                asset,
                null,
                USER_SERVICE,
                TARGET_USER,
                event.getId(),
                USAGE_USER_COVER,
                event.getVersion(),
                event.getUpdatedById());
    }

    @Override
    @Transactional
    public void removePrimaryUserCoverLink(UUID userId, Long sourceVersion) {
        removePrimaryLinkIfNewer(
                null,
                USER_SERVICE,
                TARGET_USER,
                userId,
                USAGE_USER_COVER,
                sourceVersion);
    }

    private Optional<FileAssetLink> findPrimarySlotForUpdate(
            String targetService,
            String targetType,
            UUID targetId,
            String usageType) {
        return linkRepository.findPrimarySlotForUpdate(targetService, targetType, targetId, usageType);
    }

    private boolean hasAlreadyProcessed(Optional<FileAssetLink> existing, Long sourceVersion) {
        return existing.map(link -> link.hasProcessedSourceVersion(sourceVersion)).orElse(false);
    }

    private void savePrimaryLink(
            Optional<FileAssetLink> existing,
            FileAsset asset,
            UUID workspaceId,
            String targetService,
            String targetType,
            UUID targetId,
            String usageType,
            Long sourceVersion,
            UUID linkedByUserId) {
        existing.ifPresentOrElse(link -> link.replaceAssetIfNewer(asset.getId(), linkedByUserId, sourceVersion),
                () -> insertPrimaryLink(asset, workspaceId, targetService, targetType, targetId, usageType,
                        sourceVersion, linkedByUserId));
    }

    private void insertPrimaryLink(FileAsset asset, UUID workspaceId, String targetService, String targetType,
            UUID targetId, String usageType, Long sourceVersion, UUID linkedByUserId) {
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

    private void removePrimaryLinkIfNewer(
            UUID workspaceId,
            String targetService,
            String targetType,
            UUID targetId,
            String usageType,
            Long sourceVersion) {
        Assert.notNull(targetId, "targetId is required");
        Assert.notNull(sourceVersion, "sourceVersion is required");

        Optional<FileAssetLink> existing = findPrimarySlotForUpdate(targetService, targetType, targetId, usageType);

        if (hasAlreadyProcessed(existing, sourceVersion)) {
            return;
        }

        existing.ifPresentOrElse(link -> link.markDeletedIfNewer(sourceVersion, clock),
                () -> insertDeletedPrimarySlot(workspaceId, targetService, targetType, targetId, usageType,
                        sourceVersion));
    }

    private void insertDeletedPrimarySlot(
            UUID workspaceId,
            String targetService,
            String targetType,
            UUID targetId,
            String usageType,
            Long sourceVersion) {
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
        Assert.notNull(assetId, "assetId is required");

        return assetRepository.findByIdAndDeletedFlagFalse(assetId)
                .orElseThrow(() -> FileException.assetNotFound(assetId));
    }

}
