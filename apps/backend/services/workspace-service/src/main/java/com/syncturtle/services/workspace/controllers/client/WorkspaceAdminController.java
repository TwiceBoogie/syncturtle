package com.syncturtle.services.workspace.controllers.client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.web.pagination.CursorPageResponse;
import com.syncturtle.services.workspace.application.query.WorkspaceQueryHandler;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceSlugCheckResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/instances/workspaces")
@RequiredArgsConstructor
public class WorkspaceAdminController {

    private final WorkspaceQueryHandler query;

    @GetMapping("/")
    public ResponseEntity<CursorPageResponse<WorkspaceResponse>> workspaceAll(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int perPage,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(query.workspaceAll(cursor, perPage, search));
    }

    @PostMapping("/")
    public ResponseEntity<WorkspaceResponse> workspaceCreate(@Valid @RequestBody WorkspaceCreateRequest request) {
        return ResponseEntity.ok(query.workspaceCreate(request));
    }

    @GetMapping("/slug-check/")
    public ResponseEntity<WorkspaceSlugCheckResponse> workspaceSlugCheck(@RequestParam(required = true) String slug) {
        return ResponseEntity.ok(query.workspaceSlugCheck(slug));
    }

}
