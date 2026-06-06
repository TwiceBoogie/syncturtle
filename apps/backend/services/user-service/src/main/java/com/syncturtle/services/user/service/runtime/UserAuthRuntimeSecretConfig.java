package com.syncturtle.services.user.service.runtime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserAuthRuntimeSecretConfig {
    String googleClientId;
    String googleClientSecret;

    String githubClientId;
    String githubClientSecret;
    String githubAppName;

    String gitlabHost;
    String gitlabClientId;
    String gitlabClientSecret;

    Long version;
}
