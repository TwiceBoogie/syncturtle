package com.syncturtle.services.user.service.authentication.oauth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OAuthAuthenticationSpec {
    String code;
    String ipAddress;
    String userAgent;
}
