package com.syncturtle.services.file.service.impl;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.file.exception.FileException;
import com.syncturtle.services.file.dto.response.StaticAssetUrlResponse;
import com.syncturtle.services.file.model.FileAsset;
import com.syncturtle.services.file.repository.FileAssetLinkRepository;
import com.syncturtle.services.file.repository.FileAssetRepository;
import com.syncturtle.services.file.service.StaticAssetService;
import com.syncturtle.services.file.service.collaborator.asset.FileAssetPolicy;
import com.syncturtle.services.file.service.collaborator.storage.ObjectStorageGateway;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaticAssetServiceImpl implements StaticAssetService {

    private static final Duration STATIC_ASSET_URL_TTL = Duration.ofMinutes(10);

    private final FileAssetRepository assetRepository;
    private final FileAssetLinkRepository linkRepository;
    private final FileAssetPolicy assetPolicy;
    private final ObjectStorageGateway storage;

    @Override
    public StaticAssetUrlResponse signedStaticAssetUrl(UUID currentUserId, UUID assetId) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(assetId, "assetId is required");

        FileAsset asset = assetRepository.findByIdAndDeletedFlagFalse(assetId)
                .orElseThrow(() -> FileException.assetNotFound(assetId));

        assetPolicy.requireStaticDisplayAsset(asset);

        boolean owner = asset.isOwnedBy(currentUserId);
        boolean linked = linkRepository.existsByAssetIdAndDeletedAtIsNull(assetId);
        if (!owner && !linked) {
            throw FileException.assetNotFound(assetId);
        }

        try {
            String signedUrl = storage.generatePresignedUrl(
                    asset.getObjectKey(),
                    asset.getStorageVersionId(),
                    STATIC_ASSET_URL_TTL,
                    "inline",
                    asset.getOriginalFilename());
            return new StaticAssetUrlResponse(signedUrl);
        } catch (FileException exception) {
            throw exception;
        } catch (Exception exception) {
            throw FileException.assetSignedUrlFailed(assetId, exception);
        }
    }

}
