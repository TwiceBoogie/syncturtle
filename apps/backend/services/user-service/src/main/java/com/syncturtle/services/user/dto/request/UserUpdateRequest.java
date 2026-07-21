package com.syncturtle.services.user.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UserUpdateRequest {
    @NotBlank(message = "firstName is required")
    String firstName;

    @NotBlank(message = "lastName is required")
    String lastName;
    UUID avatarAssetId;
}
