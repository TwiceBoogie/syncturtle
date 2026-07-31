package com.syncturtle.services.user.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.core.asset.AssetContentUrlFactory;
import com.syncturtle.services.user.dto.response.UserMeProfileResponse;
import com.syncturtle.services.user.dto.response.UserMeResponse;
import com.syncturtle.services.user.dto.response.UserMeSettingsResponse;
import com.syncturtle.services.user.dto.response.UserMeSettingsWorkspaceResponse;
import com.syncturtle.services.user.model.Profile;
import com.syncturtle.services.user.model.User;
import com.syncturtle.services.user.repository.projection.ProfileMeProjection;
import com.syncturtle.services.user.repository.projection.UserSettingsIdentityProjection;
import com.syncturtle.services.user.repository.projection.UserSettingsWorkspaceProjection;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserApiMapper {

    private final AssetContentUrlFactory assetUrlFactory;

    public UserMeResponse toMe(User user) {
        Assert.notNull(user, "user is required");
        return UserMeResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .mobileNumber(user.getMobileNumber())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(assetUrlFactory.staticAssetUrl(user.getAvatarAssetId()))
                .coverImageUrl(assetUrlFactory.staticAssetUrl(user.getCoverImageAssetId()))
                .avatarAssetId(user.getAvatarAssetId())
                .coverImageAssetId(user.getCoverImageAssetId())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .passwordAutoset(user.isPasswordAutoset())
                .tourCompleted(false)
                .bot(false)
                .lastLoginMedium(user.getLastLoginMedium())
                .createdAt(user.getCreatedAt())
                .lastWorkspaceId(null)
                .build();
    }

    public UserMeProfileResponse toProfile(ProfileMeProjection projection) {
        return UserMeProfileResponse.builder()
                .id(projection.getId())
                .user(projection.getUserId())
                .role(projection.getRole())
                .lastWorkspaceId(projection.getLastWorkspaceId())
                .theme(projection.getTheme())
                .onboardingStep(projection.getOnboardingStep())
                .onBoarded(projection.isOnboarded())
                .tourCompleted(projection.isTourCompleted())
                .useCase(projection.getUseCase())
                .billingAddressCountry(projection.getBillingAddressCountry())
                .billingAddress(projection.getBillingAddress())
                .hasBillingAddress(projection.isHasBillingAddress())
                .marketingEmailConsent(projection.isMarketingEmailConsent())
                .language(projection.getLanguage())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())
                .build();
    }

    public UserMeProfileResponse toProfile(Profile profile, UUID userId) {
        return UserMeProfileResponse.builder()
                .id(profile.getId())
                .user(userId)
                .role(profile.getRole())
                .lastWorkspaceId(profile.getLastWorkspaceId())
                .theme(profile.getTheme())
                .onboardingStep(profile.getOnboardingStep())
                .onBoarded(profile.isOnboarded())
                .tourCompleted(profile.isTourCompleted())
                .useCase(profile.getUseCase())
                .billingAddressCountry(profile.getBillingAddressCountry())
                .billingAddress(profile.getBillingAddress())
                .hasBillingAddress(profile.hasBillingAddress())
                .marketingEmailConsent(profile.isMarketingEmailConsent())
                .language(profile.getLanguage())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    public UserMeSettingsResponse toSettings(UserSettingsIdentityProjection user,
            UserMeSettingsWorkspaceResponse workspace) {
        return UserMeSettingsResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .workspace(workspace)
                .build();
    }

    public UserMeSettingsWorkspaceResponse toUserMeSettingsWorkspaceResponse(UserSettingsWorkspaceProjection workspace,
            long invites) {
        return UserMeSettingsWorkspaceResponse.builder()
                .lastWorkspaceId(workspace.getId())
                .lastWorkspaceSlug(workspace.getSlug())
                .lastWorkspaceName(workspace.getName())
                .lastWorkspaceLogo(assetUrlFactory.staticAssetUrl(workspace.getLogoAssetId()))
                .fallbackWorkspaceId(workspace.getId())
                .fallbackWorkspaceSlug(workspace.getSlug())
                .invites(invites)
                .build();
    }

    public UserMeSettingsWorkspaceResponse fromFallbackWorkspace(UserSettingsWorkspaceProjection workspace,
            long invites) {
        return UserMeSettingsWorkspaceResponse.builder()
                .lastWorkspaceId(null)
                .lastWorkspaceSlug(null)
                .lastWorkspaceName(null)
                .lastWorkspaceLogo(null)
                .fallbackWorkspaceId(workspace == null ? null : workspace.getId())
                .fallbackWorkspaceSlug(workspace == null ? null : workspace.getSlug())
                .invites(invites)
                .build();
    }

}
