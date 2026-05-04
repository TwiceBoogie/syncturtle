package com.syncturtle.common.contracts.auth.session;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class IssueSessionResponse {
    String accessToken;
    String refreshToken;
    Instant accessIssuedAt;
    Instant accessExpiresAt;
    Instant refreshIssuedAt;
    Instant refreshExpiresAt;
}
