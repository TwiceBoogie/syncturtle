package com.syncturtle.services.user.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthRuntimeSecretConfig {
    private String googleClientId;
    private String googleClientSecret;

    private String githubClientId;
    private String githubClientSecret;
    private String githubAppName;

    private String gitlabHost;
    private String gitlabClientId;
    private String gitlabClientSecret;

    private long version;
}
