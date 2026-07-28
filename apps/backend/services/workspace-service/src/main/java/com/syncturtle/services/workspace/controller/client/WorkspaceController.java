package com.syncturtle.services.workspace.controller.client;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.services.workspace.dto.request.WorkspaceCreateRequest;
import com.syncturtle.services.workspace.dto.request.WorkspaceInvitationRequest;
import com.syncturtle.services.workspace.dto.response.SimpleMessageResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.service.WorkspaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService service;

    @PostMapping("/{workspaceSlug}/invitations")
    public ResponseEntity<SimpleMessageResponse> createWorkspaceInvitations(
            @CurrentUser UUID currentUserId,
            @Valid @RequestBody WorkspaceInvitationRequest request,
            @PathVariable String workspaceSlug) {
        return ResponseEntity.ok(service.createWorkspaceInvitations(currentUserId, workspaceSlug, request));
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(@CurrentUser UUID currentUserId,
            @Valid @RequestBody WorkspaceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.workspaceCreate(currentUserId, request));
    }

}
