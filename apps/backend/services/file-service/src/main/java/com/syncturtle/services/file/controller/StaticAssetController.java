package com.syncturtle.services.file.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.services.file.service.StaticAssetService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StaticAssetController {

    private final StaticAssetService service;

    @GetMapping("/api/assets/v1/static/{assetId}")
    public ResponseEntity<Void> redirectToStaticAsset(@PathVariable UUID assetId) {
        String signedUrl = service.signedStaticAssetUrl(assetId);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(signedUrl).toString())
                .build();
    }

}
