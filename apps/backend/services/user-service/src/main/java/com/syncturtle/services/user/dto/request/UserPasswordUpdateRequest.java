package com.syncturtle.services.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UserPasswordUpdateRequest {
    @NotBlank(message = "password is required")
    String password;
}
