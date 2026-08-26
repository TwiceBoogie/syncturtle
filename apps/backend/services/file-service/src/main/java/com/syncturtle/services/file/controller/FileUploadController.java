package com.syncturtle.services.file.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.services.file.dto.request.FileUploadCreateRequest;
import com.syncturtle.services.file.dto.response.FileUploadCreateResponse;
import com.syncturtle.services.file.service.FileUploadService;
import com.syncturtle.services.file.service.param.FileUploadCreateParam;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/assets/v1")
@RequiredArgsConstructor
public class FileUploadController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final FileUploadService service;

    @PostMapping("/uploads")
    public ResponseEntity<FileUploadCreateResponse> createUpload(
            @CurrentUser UUID currentUserId,
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody FileUploadCreateRequest request) {
        FileUploadCreateParam param = FileUploadCreateParam.builder()
                .currentUserId(currentUserId)
                .idempotencyKey(idempotencyKey)
                .purpose(request.getPurpose())
                .workspaceId(request.getWorkspaceId())
                .originalFilename(request.getOriginalFilename())
                .contentType(request.getContentType())
                .sizeBytes(request.getSizeBytes())
                .build();

        return ResponseEntity.ok(service.createUpload(param));
    }

    @PatchMapping("/uploads/{assetId}/complete")
    public ResponseEntity<Void> completeUpload(@CurrentUser UUID currentUserId, @PathVariable UUID assetId) {
        service.completeUpload(currentUserId, assetId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> deleteAsset(@CurrentUser UUID currentUserId, @PathVariable UUID assetId) {
        service.deleteAsset(currentUserId, assetId);
        return ResponseEntity.noContent().build();
    }

}
