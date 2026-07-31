package com.syncturtle.services.file.controller.internal;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.contracts.file.response.FileAssetValidationResponse;
import com.syncturtle.services.file.service.FileAssetInternalService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FileAssetInternalController {

    private final FileAssetInternalService service;

    @GetMapping("/internal/v1/assets/{assetId}/validation")
    public FileAssetValidationResponse validateAsset(@PathVariable UUID assetId) {
        return service.validateAsset(assetId);
    }

}
