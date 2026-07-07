package com.syncturtle.services.user.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.syncturtle.common.core.service.ServiceClientNames;
import com.syncturtle.services.user.dto.response.FileAssetValidationResponse;

@FeignClient(value = ServiceClientNames.FILE_SERVICE, contextId = "userFileClient", url = "${app.services.file-service.base-url}", path = "/internal/v1/assets")
public interface FileClient {

    @GetMapping("/{assetId}/validation")
    FileAssetValidationResponse validateAsset(@PathVariable UUID assetId);

}
