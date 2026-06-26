package com.syncturtle.services.user.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UserCoverImageUpdateRequest {
    @NotNull
    UUID assetId;
}
