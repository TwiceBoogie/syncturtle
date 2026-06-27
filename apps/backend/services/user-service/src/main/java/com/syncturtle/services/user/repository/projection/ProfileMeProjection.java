package com.syncturtle.services.user.repository.projection;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public interface ProfileMeProjection {
    UUID getId();

    ProfileUserProjection getUser();

    String getRole();

    UUID getLastWorkspaceId();

    JsonNode getTheme();

    JsonNode getOnboardingStep();

    boolean isOnboarded();

    boolean isTourCompleted();

    String getUseCase();

    String getBillingAddressCountry();

    JsonNode getBillingAddress();

    boolean isHasBillingAddress();

    boolean isMarketingEmailConsent();

    String getLanguage();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    interface ProfileUserProjection {
        UUID getId();
    }

    default UUID getUserId() {
        return getUser().getId();
    }
}
