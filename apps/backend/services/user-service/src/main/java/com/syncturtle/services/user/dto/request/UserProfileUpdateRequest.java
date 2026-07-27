package com.syncturtle.services.user.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.JsonNode;

@Value
@Builder
@Jacksonized
public class UserProfileUpdateRequest {
    @Size(max = 300)
    String role;
    JsonNode theme;
    JsonNode onboardingStep;
    JsonNode billingAddress;
    String useCase;
    @Size(max = 255)
    String billingAddressCountry;
    Boolean hasBillingAddress;
    @Size(max = 255)
    String language;
    Boolean hasMarketingEmailConsent;
    UUID lastWorkspaceId;
}
