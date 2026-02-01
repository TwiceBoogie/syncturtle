package com.syncturtle.platform.services.instance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public final class InstanceAdminSignupForm {

    @NotBlank
    private String firstName;
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String companyName;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;
    private String telemetryEnabled;
    private String nextPath;
}
