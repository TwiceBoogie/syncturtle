package com.syncturtle.services.instance.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.core.endpoint.EndpointPaths;
import com.syncturtle.common.security.annotation.RequireInstancePermission;
import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.common.web.pagination.CursorPageResponse;
import com.syncturtle.services.instance.dto.request.InstanceWorkspaceCreateRequest;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceResponse;
import com.syncturtle.services.instance.dto.response.InstanceWorkspaceSlugCheckResponse;
import com.syncturtle.services.instance.service.InstanceWorkspaceAdminService;
import com.syncturtle.services.instance.type.InstanceAdminRoleCodes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(EndpointPaths.API_INSTANCES)
public class InstanceWorkspaceAdminController {

    private final InstanceWorkspaceAdminService service;

    @GetMapping("/workspaces/slug-check")
    @RequireInstancePermission(minRole = InstanceAdminRoleCodes.ADMIN)
    public ResponseEntity<InstanceWorkspaceSlugCheckResponse> checkWorkspaceSlug(
            @CurrentUser UUID currentUserId,
            @RequestParam String slug) {
        return ResponseEntity.ok(service.checkSlug(currentUserId, slug));
    }

    @GetMapping("/workspaces")
    @RequireInstancePermission(minRole = InstanceAdminRoleCodes.ADMIN)
    public ResponseEntity<CursorPageResponse<InstanceWorkspaceResponse>> getWorkspaces(
            @CurrentUser UUID currentUserId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int perPage,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getWorkspaces(currentUserId, cursor, normalizePerPage(perPage), search));
    }

    @PostMapping("/workspaces")
    @RequireInstancePermission(minRole = InstanceAdminRoleCodes.ADMIN)
    public ResponseEntity<InstanceWorkspaceResponse> createWorkspace(
            @CurrentUser UUID currentUserId,
            @Valid @RequestBody InstanceWorkspaceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createWorkspace(currentUserId, request));
    }

    private static int normalizePerPage(int perPage) {
        if (perPage <= 0) {
            return 10;
        }

        return Math.min(perPage, 100);
    }

}
