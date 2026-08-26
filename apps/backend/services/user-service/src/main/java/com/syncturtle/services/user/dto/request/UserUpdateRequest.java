package com.syncturtle.services.user.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "displayName is required")
    @Size(max = 120)
    String displayName;
    UUID avatarAssetId;
}
