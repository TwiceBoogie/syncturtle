package com.syncturtle.common.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class AdminSigninInternalRequest {
    private final String email;
    private final String password;
}
