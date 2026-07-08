package com.syncturtle.services.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UserOnboardUpdateRequest {
    @NotNull
    Boolean isOnboarded;
}
