package com.syncturtle.common.web.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminSignupInternalResponse {
    private final UUID userId;
}
