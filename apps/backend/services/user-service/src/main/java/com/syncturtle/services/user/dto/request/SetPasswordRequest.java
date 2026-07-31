package com.syncturtle.services.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class SetPasswordRequest {
    @NotBlank
    @Size(min = 8)
    String password;
}
