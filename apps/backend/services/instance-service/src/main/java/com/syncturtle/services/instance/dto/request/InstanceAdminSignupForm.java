package com.syncturtle.services.instance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class InstanceAdminSignupForm {

    @NotBlank(message = "Enter your first name.")
    @Size(max = 150, message = "First name must be 150 characters or fewer.")
    private String firstName;

    @Size(max = 150, message = "Last name must be 150 characters or fewer.")
    private String lastName;

    @NotBlank(message = "Enter your email address.")
    @Email(message = "Enter a valid email address, such as lunasnow@marvel.com.")
    @Size(max = 255, message = "Email address must be 255 characters or fewer.")
    private String email;

    @NotBlank(message = "Enter your company or workspace name.")
    @Size(max = 100, message = "Company name must be 100 characters or fewer.")
    private String companyName;

    @NotBlank(message = "Enter a password.")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters.")
    private String password;

    @Pattern(regexp = "^(true|false|on|off|1|0)?$", message = "Choose a valid telemetry preference.")
    private String telemetryEnabled;

    @Pattern(regexp = "^(/[^\\r\\n]*)?$", message = "Redirect path must be a local application path.")
    @Size(max = 500, message = "Redirect path must be 500 characters or fewer.")
    private String nextPath;

    public boolean isTelemetryEnabled() {
        return "true".equalsIgnoreCase(telemetryEnabled)
                || "on".equalsIgnoreCase(telemetryEnabled)
                || "1".equals(telemetryEnabled);
    }

}
