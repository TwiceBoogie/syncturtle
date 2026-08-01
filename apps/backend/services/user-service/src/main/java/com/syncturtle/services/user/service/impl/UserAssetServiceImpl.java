package com.syncturtle.services.user.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.file.FileAssetPurpose;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.services.user.dto.request.UserAvatarUpdateRequest;
import com.syncturtle.services.user.dto.request.UserCoverImageUpdateRequest;
import com.syncturtle.services.user.dto.response.UserMeResponse;
import com.syncturtle.services.user.mapper.UserApiMapper;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.service.UserAssetService;
import com.syncturtle.services.user.service.collaborator.asset.UserFileAssetVerifier;
import com.syncturtle.services.user.service.collaborator.outbox.UserOutboxWriter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAssetServiceImpl implements UserAssetService {

    private final UserRepository userRepository;
    private final UserFileAssetVerifier assetVerifier;
    private final UserEventFactory userEventFactory;
    private final UserOutboxWriter outboxWriter;
    private final UserApiMapper userApiMapper;

    @Override
    @Transactional
    public UserMeResponse updateAvatar(UUID currentUserId, UserAvatarUpdateRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(request, "user avatar update request is required");

        User user = requireUser(currentUserId);

        assetVerifier.requireValidUserAsset(request.getAssetId(), currentUserId, FileAssetPurpose.USER_AVATAR);

        user.assignAvatar(request.getAssetId());
        userRepository.flush();

        UserEvent event = userEventFactory.updated(user);
        outboxWriter.saveUserEvent(event);

        return userApiMapper.toMe(user);
    }

    @Override
    @Transactional
    public UserMeResponse clearAvatar(UUID currentUserId) {
        Assert.notNull(currentUserId, "currentUserId is required");

        User user = requireUser(currentUserId);

        user.clearAvatar();
        userRepository.flush();

        UserEvent event = userEventFactory.updated(user);
        outboxWriter.saveUserEvent(event);

        return userApiMapper.toMe(user);
    }

    @Override
    @Transactional
    public UserMeResponse updateCoverImage(UUID currentUserId, UserCoverImageUpdateRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(request, "user cover image update request is required");

        User user = requireUser(currentUserId);

        assetVerifier.requireValidUserAsset(request.getAssetId(), currentUserId, FileAssetPurpose.USER_COVER);

        user.assignCoverImage(request.getAssetId());
        userRepository.flush();

        UserEvent event = userEventFactory.updated(user);
        outboxWriter.saveUserEvent(event);

        return userApiMapper.toMe(user);
    }

    @Override
    @Transactional
    public UserMeResponse clearCoverImage(UUID currentUserId) {
        Assert.notNull(currentUserId, "currentUserId is required");

        User user = requireUser(currentUserId);

        user.clearCoverImage();
        userRepository.flush();

        UserEvent event = userEventFactory.updated(user);
        outboxWriter.saveUserEvent(event);

        return userApiMapper.toMe(user);
    }

    private User requireUser(UUID currentUserId) {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserId)
                .orElseThrow();
    }

}
