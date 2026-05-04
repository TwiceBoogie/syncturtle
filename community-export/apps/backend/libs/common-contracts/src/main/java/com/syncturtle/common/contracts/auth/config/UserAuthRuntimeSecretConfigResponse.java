package com.syncturtle.common.contracts.auth.config;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UserAuthRuntimeSecretConfigResponse {
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
