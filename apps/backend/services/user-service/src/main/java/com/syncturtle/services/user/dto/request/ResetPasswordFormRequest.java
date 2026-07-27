package com.syncturtle.services.user.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResetPasswordFormRequest {

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 256)
    String password;

    @NotBlank(message = "confirmPassword is required")
    @Size(min = 8, max = 256)
    String confirmPassword;

    @AssertTrue(message = "passwords must match")
    public boolean isPasswordConfirmationValid() {
        if (password == null || confirmPassword == null) {
            return false;
        }

        return password.equals(confirmPassword);
    }

}
