package com.syncturtle.services.user.dto.request;

import lombok.Data;

@Data
public final class SignInRequest {
    private String email;
    private String password;
    private String nextPath;
}
