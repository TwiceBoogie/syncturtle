package com.syncturtle.platform.services.user.dto.request;

import lombok.Data;

@Data
public final class SignInRequest {
    private String email;
    private String password;
}
