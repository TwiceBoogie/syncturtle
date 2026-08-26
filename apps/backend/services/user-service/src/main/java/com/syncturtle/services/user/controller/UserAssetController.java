package com.syncturtle.services.user.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.services.user.dto.request.UserAvatarUpdateRequest;
import com.syncturtle.services.user.dto.request.UserCoverImageUpdateRequest;
import com.syncturtle.services.user.dto.response.UserMeResponse;
import com.syncturtle.services.user.service.UserAssetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserAssetController {

    private final UserAssetService service;

    @PatchMapping("/api/users/me/avatar")
    public ResponseEntity<UserMeResponse> updateAvatar(
            @CurrentUser UUID currentUserId,
            @Valid @RequestBody UserAvatarUpdateRequest request) {
        return ResponseEntity.ok(service.updateAvatar(currentUserId, request));
    }

    @DeleteMapping("/api/users/me/avatar")
    public ResponseEntity<UserMeResponse> clearAvatar(@CurrentUser UUID currentUserId) {
        return ResponseEntity.ok(service.clearAvatar(currentUserId));
    }

    @PatchMapping("/api/users/me/cover-image")
    public ResponseEntity<UserMeResponse> updateCoverImage(
            @CurrentUser UUID currentUserId,
            @Valid @RequestBody UserCoverImageUpdateRequest request) {
        return ResponseEntity.ok(service.updateCoverImage(currentUserId, request));
    }

    @DeleteMapping("/api/users/me/cover-image")
    public ResponseEntity<UserMeResponse> clearCoverImage(@CurrentUser UUID currentUserId) {
        return ResponseEntity.ok(service.clearCoverImage(currentUserId));
    }

}
