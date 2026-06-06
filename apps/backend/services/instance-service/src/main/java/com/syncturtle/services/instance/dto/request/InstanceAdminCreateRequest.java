package com.syncturtle.services.instance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InstanceAdminCreateRequest {
    @Email(message = "Enter the user's email address.")
    @NotBlank(message = "Enter a valid email address, such as lunasnow@marvel.com")
    @Size(max = 255, message = "Email address must be 255 characters or fewer")
    String email;
}
