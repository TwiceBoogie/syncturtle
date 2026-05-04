package com.syncturtle.services.user.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IssueTokenResponse {
    String accessToken;
    String refreshToken;
    Instant accessIssuedAt;
    Instant accessExpiresAt;
    Instant refreshIssuedAt;
    Instant refreshExpiresAt;
}
