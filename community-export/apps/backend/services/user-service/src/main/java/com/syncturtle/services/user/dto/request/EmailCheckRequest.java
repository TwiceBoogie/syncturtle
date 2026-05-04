package com.syncturtle.services.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public final class EmailCheckRequest {
    @Email
    @NotBlank
    private String email;
}
