package com.syncturtle.services.user.dto.internal;

import com.syncturtle.services.user.payload.IssuedPassport;
import com.syncturtle.services.user.payload.IssuedRefreshToken;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class IssuedTokenPair {
    private final IssuedPassport access;
    private final IssuedRefreshToken refresh;
}
