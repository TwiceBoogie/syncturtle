package com.syncturtle.services.user.service.authentication.oauth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OAuthState {
    String state;
    String nextPath;
}
