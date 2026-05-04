package com.syncturtle.services.user.dto.internal;

import com.syncturtle.services.user.payload.IssuedPassport;
import com.syncturtle.services.user.payload.IssuedRefreshToken;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BootstrapExchangeResult {
    private final IssuedPassport access;
    private final IssuedRefreshToken refresh;
    private final String redirectionLocation;
}
