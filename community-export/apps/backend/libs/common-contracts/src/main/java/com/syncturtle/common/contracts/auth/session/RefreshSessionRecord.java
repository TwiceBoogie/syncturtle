package com.syncturtle.common.contracts.auth.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshSessionRecord {

    private String userId;
    private String instanceId;
    private String email;

    @Builder.Default
    private List<String> roles = new ArrayList<>();

    private Long authVersion;
    private Long adminSessionVersion;

    private boolean active;

    private String refreshTokenHash;

    private String ipAddress;
    private String userAgent;

    private Instant issuedAt;
    private Instant expiresAt;

    public List<String> safeRoles() {
        if (roles == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(roles);
    }
}