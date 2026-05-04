package com.syncturtle.services.email.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class EmailCredentialCheckRequest {
    @NotBlank(message = "Receiver email is required")
    @Email(message = "Receiver email must be a valid email address")
    private final String receiverEmail;
}
