package com.syncturtle.services.user.service;

import java.util.UUID;

import com.syncturtle.services.user.dto.request.UserOnboardUpdateRequest;
import com.syncturtle.services.user.dto.request.UserProfileUpdateRequest;
import com.syncturtle.services.user.dto.request.UserUpdateRequest;
import com.syncturtle.services.user.dto.response.UserMeProfileResponse;
import com.syncturtle.services.user.dto.response.UserMeResponse;
import com.syncturtle.services.user.dto.response.UserMeSettingsResponse;

public interface UserService {
    UserMeResponse getMe(UUID currentUserId);

    UserMeResponse updateMe(UUID currentUserId, UserUpdateRequest request);

    UserMeProfileResponse getProfile(UUID currentUserId);

    UserMeSettingsResponse getSettings(UUID currentUserId);

    UserMeProfileResponse updateUserProfile(UUID currentUserId, UserProfileUpdateRequest request);

    void updateUserOnboard(UUID currentUserId, UserOnboardUpdateRequest request);

    UserMeProfileResponse updateUserTourCompleted(UUID currentUserId);
}
