package com.syncturtle.services.user.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.JsonNode;

@Value
@Builder
public class UserMeProfileResponse {
    UUID id;
    UUID user;
    String role;
    UUID lastWorkspaceId;
    JsonNode theme;
    JsonNode onboardingStep;
    @JsonProperty("isOnboarded")
    boolean onBoarded;
    @JsonProperty("isTourCompleted")
    boolean tourCompleted;
    String useCase;
    String billingAddressCountry;
    JsonNode billingAddress;
    boolean hasBillingAddress;
    @JsonProperty("hasMarketingEmailConsent")
    boolean marketingEmailConsent;
    String language;
    Instant createdAt;
    Instant updatedAt;
}
