package com.syncturtle.services.workspace.controller.internal;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;
import com.syncturtle.services.workspace.service.WorkspaceAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/instance-workspaces")
public class WorkspaceAdminInternalController {

    private final WorkspaceAdminService service;

    @GetMapping("/slug-check")
    public WorkspaceSlugCheckResponse checkSlug(
            @CurrentUser UUID currentUserId,
            @RequestParam String slug) {
        return service.checkSlug(currentUserId, slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse createWorkspace(
            @CurrentUser UUID currentUserId,
            @Valid @RequestBody WorkspaceCreateRequest request) {
        return service.createWorkspace(currentUserId, request);
    }

}
