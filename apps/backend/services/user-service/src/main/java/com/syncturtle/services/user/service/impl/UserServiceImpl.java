package com.syncturtle.services.user.service.impl;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.contracts.user.exception.UserException;
import com.syncturtle.services.user.dto.request.UserOnboardUpdateRequest;
import com.syncturtle.services.user.dto.request.UserProfileUpdateRequest;
import com.syncturtle.services.user.dto.request.UserUpdateRequest;
import com.syncturtle.services.user.dto.response.UserMeProfileResponse;
import com.syncturtle.services.user.dto.response.UserMeResponse;
import com.syncturtle.services.user.dto.response.UserMeSettingsResponse;
import com.syncturtle.services.user.dto.response.UserMeSettingsWorkspaceResponse;
import com.syncturtle.services.user.mapper.UserApiMapper;
import com.syncturtle.services.user.messaging.kafka.factory.UserEventFactory;
import com.syncturtle.services.user.model.Profile;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.model.param.UserProfileUpdateParam;
import com.syncturtle.services.user.model.param.UserUpdateParam;
import com.syncturtle.services.user.repository.ProfileRepository;
import com.syncturtle.services.user.repository.UserRepository;
import com.syncturtle.services.user.repository.WorkspaceMemberInviteLiteRepository;
import com.syncturtle.services.user.repository.WorkspaceMemberLiteRepository;
import com.syncturtle.services.user.repository.projection.ProfileMeProjection;
import com.syncturtle.services.user.repository.projection.UserSettingsIdentityProjection;
import com.syncturtle.services.user.repository.projection.UserSettingsProfileProjection;
import com.syncturtle.services.user.repository.projection.UserSettingsWorkspaceProjection;
import com.syncturtle.services.user.service.UserService;
import com.syncturtle.services.user.service.collaborator.outbox.UserOutboxWriter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final WorkspaceMemberLiteRepository workspaceMemberRepository;
    private final WorkspaceMemberInviteLiteRepository workspaceMemberInviteRepository;
    private final UserEventFactory userEventFactory;
    private final UserOutboxWriter outboxWriter;
    private final UserApiMapper userApiMapper;

    @Override
    @Transactional(readOnly = true)
    public UserMeResponse getMe(UUID currentUserId) {
        Assert.notNull(currentUserId, "currentUserId is required");

        User user = userRepository.findById(currentUserId).orElseThrow();
        return userApiMapper.toMe(user);
    }

    @Override
    @Transactional
    public UserMeResponse updateMe(UUID currentUserId, UserUpdateRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(request, "user update request is required");

        User user = userRepository.findById(currentUserId).orElseThrow(() -> UserException.userNotFound(currentUserId));

        UserUpdateParam param = UserUpdateParam.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .avatarAssetId(request.getAvatarAssetId())
                .build();

        user.update(param);
        userRepository.flush();

        UserEvent event = userEventFactory.updated(user);
        outboxWriter.saveUserEvent(event);

        return userApiMapper.toMe(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserMeProfileResponse getProfile(UUID currentUserId) {
        Assert.notNull(currentUserId, "currentUserId is required");

        ProfileMeProjection profile = getCurrentUserProfile(currentUserId, ProfileMeProjection.class);
        return userApiMapper.toProfile(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserMeSettingsResponse getSettings(UUID currentUserId) {
        Assert.notNull(currentUserId, "currentUserId is required");

        UserSettingsIdentityProjection user = userRepository.findSettingsIdentityById(currentUserId)
                .orElseThrow(() -> UserException.userNotFound(currentUserId));
        UserSettingsProfileProjection profile = getCurrentUserProfile(currentUserId,
                UserSettingsProfileProjection.class);
        long invites = workspaceMemberInviteRepository
                .countByEmailAndAcceptedFalseAndDeletedAtIsNull(normalizeEmail(user.getEmail()));

        UserMeSettingsWorkspaceResponse workspace = resolveWorkspaceSettings(currentUserId,
                profile.getLastWorkspaceId(), invites);

        return userApiMapper.toSettings(user, workspace);
    }

    @Override
    @Transactional
    public UserMeProfileResponse updateUserProfile(UUID currentUserId, UserProfileUpdateRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(request, "user profile update request is required");

        Profile profile = getCurrentUserProfile(currentUserId, Profile.class);

        UserProfileUpdateParam param = UserProfileUpdateParam.builder()
                .role(request.getRole())
                .lastWorkspaceId(request.getLastWorkspaceId())
                .theme(request.getTheme())
                .onboardingStep(request.getOnboardingStep())
                .useCase(request.getUseCase())
                .billingAddressCountry(request.getBillingAddressCountry())
                .billingAddress(request.getBillingAddress())
                .hasBillingAddress(request.getHasBillingAddress())
                .language(request.getLanguage())
                .hasMarketingEmailConsent(request.getHasMarketingEmailConsent())
                .build();

        profile.updateProfile(param);
        return userApiMapper.toProfile(profile, currentUserId);
    }

    @Override
    @Transactional
    public void updateUserOnboard(UUID currentUserId, UserOnboardUpdateRequest request) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(request, "user onboard update request is required");

        Profile profile = getCurrentUserProfile(currentUserId, Profile.class);

        if (Boolean.TRUE.equals(request.getIsOnboarded())) {
            profile.completeOnboarding();
        }
    }

    @Override
    @Transactional
    public UserMeProfileResponse updateUserTourCompleted(UUID currentUserId) {
        Assert.notNull(currentUserId, "currentUserId is required");

        Profile profile = getCurrentUserProfile(currentUserId, Profile.class);

        profile.markTourCompleted();

        return userApiMapper.toProfile(profile, currentUserId);
    }

    private UserMeSettingsWorkspaceResponse resolveWorkspaceSettings(UUID userId, UUID lastWorkspaceId, long invites) {
        if (lastWorkspaceId != null) {
            Optional<UserSettingsWorkspaceProjection> lastWorkspace = workspaceMemberRepository
                    .findActiveWorkspaceForUser(userId, lastWorkspaceId);

            if (lastWorkspace.isPresent()) {
                return userApiMapper.toUserMeSettingsWorkspaceResponse(lastWorkspace.get(), invites);
            }
        }

        Optional<UserSettingsWorkspaceProjection> fallbackWorkspace = workspaceMemberRepository
                .findFirstActiveWorkspaceForUser(userId);

        return userApiMapper.fromFallbackWorkspace(fallbackWorkspace.orElse(null), invites);
    }

    private <T> T getCurrentUserProfile(UUID currentUserId, Class<T> clazz) {
        return profileRepository.findByUser_Id(currentUserId, clazz)
                .orElseThrow(() -> UserException.profileNotFound(currentUserId));
    }

    private static String normalizeEmail(String email) {
        Assert.hasText(email, "email is required");

        return email.trim().toLowerCase(Locale.ROOT);
    }

}
