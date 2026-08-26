package com.syncturtle.services.file.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class StaticAssetUrlResponse {
    private final String signedUrl;
}
