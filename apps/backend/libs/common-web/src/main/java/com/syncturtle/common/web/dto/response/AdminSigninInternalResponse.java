package com.syncturtle.common.web.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminSigninInternalResponse {
    private final UUID userId;
    private final Long authVersion;
}
