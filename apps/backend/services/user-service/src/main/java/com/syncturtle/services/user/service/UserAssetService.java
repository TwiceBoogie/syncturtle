package com.syncturtle.services.user.service;

import java.util.UUID;

import com.syncturtle.services.user.dto.request.UserAvatarUpdateRequest;
import com.syncturtle.services.user.dto.request.UserCoverImageUpdateRequest;
import com.syncturtle.services.user.dto.response.UserMeResponse;

public interface UserAssetService {
    UserMeResponse updateAvatar(UUID currentUserId, UserAvatarUpdateRequest request);

    UserMeResponse clearAvatar(UUID currentUserId);

    UserMeResponse updateCoverImage(UUID currentUserId, UserCoverImageUpdateRequest request);

    UserMeResponse clearCoverImage(UUID currentUserId);
}
