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
public class SignInRequest {

    @NotBlank(message = "Enter your email address.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 255, message = "Email address must be 255 characters or fewer.")
    private String email;

    @NotBlank(message = "Enter your password.")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters.")
    private String password;
    private String nextPath;

}
