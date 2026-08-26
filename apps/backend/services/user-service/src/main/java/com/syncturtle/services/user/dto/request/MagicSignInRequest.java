package com.syncturtle.services.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MagicSignInRequest {

    @NotBlank(message = "Enter your email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 255, message = "Email address must be 255 characters or fewer.")
    private String email;

    @NotBlank(message = "Code is required")
    private String code;
    private String nextPath;

}
