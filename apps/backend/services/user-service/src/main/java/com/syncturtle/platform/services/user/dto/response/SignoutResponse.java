package com.syncturtle.platform.services.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class SignoutResponse {
    private final boolean success;
    private final String redirection;
}
