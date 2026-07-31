package com.syncturtle.services.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ForgotPasswordRequest {
    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    String email;
}
