package com.syncturtle.services.file.service.impl;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.file.exception.FileException;
import com.syncturtle.services.file.model.FileAsset;
import com.syncturtle.services.file.repository.FileAssetRepository;
import com.syncturtle.services.file.service.StaticAssetService;
import com.syncturtle.services.file.service.collaborator.asset.FileAssetPolicy;
import com.syncturtle.services.file.service.collaborator.storage.ObjectStorageGateway;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaticAssetServiceImpl implements StaticAssetService {

    private static final Duration STATIC_ASSET_URL_TTL = Duration.ofMinutes(10);

    private final FileAssetRepository repository;
    private final FileAssetPolicy assetPolicy;
    private final ObjectStorageGateway storage;

    @Override
    public String signedStaticAssetUrl(UUID assetId) {
        Assert.notNull(assetId, "assetId is required");

        FileAsset asset = repository.findByIdAndDeletedFlagFalse(assetId)
                .orElseThrow(() -> FileException.assetNotFound(assetId));

        assetPolicy.requireStaticDisplayAsset(asset);

        try {
            return storage.generatePresignedUrl(asset.getObjectKey(), STATIC_ASSET_URL_TTL, "inline",
                    asset.getOriginalFilename());
        } catch (FileException exception) {
            throw exception;
        } catch (Exception exception) {
            throw FileException.assetSignedUrlFailed(assetId, exception);
        }
    }

}
