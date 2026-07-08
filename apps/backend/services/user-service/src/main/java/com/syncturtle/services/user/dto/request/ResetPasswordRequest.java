package com.syncturtle.services.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ResetPasswordRequest {

    @NotBlank(message = "uidb64 is required")
    String uidb64;
    @NotBlank(message = "token is required")
    String token;
    @NotBlank(message = "password is required")
    String password;
    String nextPath;

}
