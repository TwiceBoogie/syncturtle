package com.syncturtle.services.file.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.file.exception.FileException;
import com.syncturtle.services.file.dto.response.FileAssetValidationResponse;
import com.syncturtle.services.file.model.FileAsset;
import com.syncturtle.services.file.repository.FileAssetRepository;
import com.syncturtle.services.file.service.FileAssetInternalService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileAssetInternalServiceImpl implements FileAssetInternalService {

    private final FileAssetRepository repository;

    @Override
    @Transactional(readOnly = true)
    public FileAssetValidationResponse validateAsset(UUID assetId) {
        Assert.notNull(assetId, "assetId is required");

        FileAsset asset = repository.findById(assetId)
                .orElseThrow(() -> FileException.assetNotFound(assetId));

        return FileAssetValidationResponse.builder()
                .id(asset.getId())
                .workspaceId(asset.getWorkspaceId())
                .ownerUserId(asset.getOwnerUserId())
                .purpose(asset.getPurpose())
                .uploaded(asset.isUploaded())
                .deleted(asset.isDeleted())
                .build();
    }

}
