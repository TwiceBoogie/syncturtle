package com.syncturtle.services.workspace.controller;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.services.workspace.dto.response.WorkspaceMemberInvitationResponse;
import com.syncturtle.services.workspace.dto.response.WorkspaceResponse;
import com.syncturtle.services.workspace.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserWorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping("/me/workspaces")
    public ResponseEntity<List<WorkspaceResponse>> getCurrentUserWorkspace(@CurrentUser UUID currentUserId) {
        return ResponseEntity.ok()
                .cacheControl(privateUserCache())
                .varyBy(HttpHeaders.COOKIE)
                .body(workspaceService.getCurrentUserWorkspaces(currentUserId));
    }

    @GetMapping("/me/workspaces/invitations")
    public ResponseEntity<List<WorkspaceMemberInvitationResponse>> listCurrentUserInvitations(
            @CurrentUser UUID currentUserId) {
        return ResponseEntity.ok()
                .cacheControl(privateUserCache())
                .varyBy(HttpHeaders.COOKIE)
                .body(workspaceService.listCurrentUserInvitations(currentUserId));
    }

    private static CacheControl privateUserCache() {
        return CacheControl.maxAge(Duration.ofSeconds(12)).cachePrivate();
    }

}
