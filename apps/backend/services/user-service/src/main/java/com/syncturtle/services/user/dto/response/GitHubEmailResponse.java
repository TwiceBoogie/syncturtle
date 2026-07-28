package com.syncturtle.services.user.dto.response;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitHubEmailResponse {
    String email;
    boolean primary;
    boolean verified;
    String visibility;
}
