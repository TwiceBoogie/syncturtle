package com.syncturtle.services.user.service.param;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OAuthAuthenticationParam {
    String code;
    String ipAddress;
    String userAgent;
}
