package com.syncturtle.common.contracts.instance.admin;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminSigninResponse {
    private final UUID userId;
    private final Long authVersion;
}
