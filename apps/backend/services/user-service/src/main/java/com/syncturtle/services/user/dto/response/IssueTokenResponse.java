package com.syncturtle.services.user.dto.response;

import java.time.Instant;

import com.syncturtle.services.user.service.collaborator.session.AuthenticatedSessionReceipt;

import lombok.Builder;
import lombok.Value;

@Value
public class IssueTokenResponse {
    String accessToken;
    String refreshToken;
    Instant accessIssuedAt;
    Instant accessExpiresAt;
    Instant refreshIssuedAt;
    Instant refreshExpiresAt;
    String location;

    @Builder
    private IssueTokenResponse(
            String accessToken,
            String refreshToken,
            Instant accessIssuedAt,
            Instant accessExpiresAt,
            Instant refreshIssuedAt,
            Instant refreshExpiresAt,
            String location) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessIssuedAt = accessIssuedAt;
        this.accessExpiresAt = accessExpiresAt;
        this.refreshIssuedAt = refreshIssuedAt;
        this.refreshExpiresAt = refreshExpiresAt;
        this.location = location;
    }

    public static IssueTokenResponse redirect(String location) {
        return IssueTokenResponse.builder()
                .location(location)
                .build();
    }

    public static IssueTokenResponse issued(AuthenticatedSessionReceipt session, String location) {
        return IssueTokenResponse.builder()
                .accessToken(session.getAccessToken().getToken())
                .refreshToken(session.getRefreshToken().getToken())
                .accessIssuedAt(session.getAccessToken().getIssuedAt())
                .accessExpiresAt(session.getAccessToken().getExpiresAt())
                .refreshIssuedAt(session.getRefreshToken().getIssuedAt())
                .refreshExpiresAt(session.getRefreshToken().getExpiresAt())
                .location(location)
                .build();
    }
}
