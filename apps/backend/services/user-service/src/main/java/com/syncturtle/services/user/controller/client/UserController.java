package com.syncturtle.services.user.controller.client;

import java.time.Duration;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncturtle.common.web.annotation.CurrentUser;
import com.syncturtle.services.user.dto.request.UserOnboardUpdateRequest;
import com.syncturtle.services.user.dto.request.UserProfileUpdateRequest;
import com.syncturtle.services.user.dto.request.UserUpdateRequest;
import com.syncturtle.services.user.dto.response.SimpleMessageResponse;
import com.syncturtle.services.user.dto.response.UserMeProfileResponse;
import com.syncturtle.services.user.dto.response.UserMeResponse;
import com.syncturtle.services.user.dto.response.UserMeSettingsResponse;
import com.syncturtle.services.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserMeResponse> getMe(@CurrentUser UUID currentUserId) {
        return ResponseEntity.ok()
                .cacheControl(privateUserCache())
                .varyBy(HttpHeaders.COOKIE)
                .body(userService.getMe(currentUserId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserMeResponse> updateMe(@CurrentUser UUID currentUserId,
            @Valid @RequestBody UserUpdateRequest request) {
        return null;
    }

    @GetMapping("/me/profile")
    public ResponseEntity<UserMeProfileResponse> getProfile(@CurrentUser UUID currentUserId) {
        return ResponseEntity.ok()
                .cacheControl(privateUserCache())
                .varyBy(HttpHeaders.COOKIE)
                .body(userService.getProfile(currentUserId));
    }

    @GetMapping("/me/settings")
    public ResponseEntity<UserMeSettingsResponse> getSettings(@CurrentUser UUID currentUserId) {
        return ResponseEntity.ok()
                .cacheControl(privateUserCache())
                .varyBy(HttpHeaders.COOKIE)
                .body(userService.getSettings(currentUserId));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<UserMeProfileResponse> updateUserProfile(@CurrentUser UUID currentUserId,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok()
                .cacheControl(privateUserCache())
                .varyBy(HttpHeaders.COOKIE)
                .body(userService.updateUserProfile(currentUserId, request));
    }

    @PatchMapping("/me/onboard")
    public ResponseEntity<SimpleMessageResponse> updateUeserOnboard(@CurrentUser UUID currentUserId,
            @Valid @RequestBody UserOnboardUpdateRequest request) {
        userService.updateUserOnboard(currentUserId, request);
        return ResponseEntity.ok(SimpleMessageResponse.updated());
    }

    @PatchMapping("/me/profile/tour-completed")
    public ResponseEntity<UserMeProfileResponse> updateUserTourCompleted(@CurrentUser UUID currentUserId) {
        return ResponseEntity.ok()
                .cacheControl(privateUserCache())
                .varyBy(HttpHeaders.COOKIE)
                .body(userService.updateUserTourCompleted(currentUserId));
    }

    private static CacheControl privateUserCache() {
        return CacheControl.maxAge(Duration.ofSeconds(12)).cachePrivate();
    }

}
