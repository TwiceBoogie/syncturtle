package com.syncturtle.services.workspace.controller.client;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.services.workspace.dto.request.WorkspaceLogoUpdateRequest;
import com.syncturtle.services.workspace.service.WorkspaceAssetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceAssetController {

    private final WorkspaceAssetService service;

    @PatchMapping("/{workspaceSlug}/logo")
    public ResponseEntity<Void> updateLogo(@CurrentUser UUID currentUserId, @PathVariable String workspaceSlug,
            @Valid @RequestBody WorkspaceLogoUpdateRequest request) {
        service.updateLogo(currentUserId, workspaceSlug, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workspaceSlug}/logo")
    public ResponseEntity<Void> clearLogo(@CurrentUser UUID currentUserId, @PathVariable String workspaceSlug) {
        service.clearLogo(currentUserId, workspaceSlug);
        return ResponseEntity.noContent().build();
    }

}
