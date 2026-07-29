package com.syncturtle.services.user.service.collaborator.authentication.oauth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OAuthState {
    String state;
    String nextPath;
}
