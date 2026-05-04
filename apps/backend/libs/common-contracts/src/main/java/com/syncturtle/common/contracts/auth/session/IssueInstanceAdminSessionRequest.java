package com.syncturtle.common.contracts.auth.session;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class IssueInstanceAdminSessionRequest {
    UUID userId;
    UUID instanceId;
    Long userAuthVersion;
    Long adminSessionVersion;
    String clientIp;
    String userAgent;
}
