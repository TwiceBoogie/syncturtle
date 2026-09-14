package com.syncturtle.services.instance.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InstanceSetupConfigResponse {
    boolean enableSignup;
    @JsonProperty("isWorkspaceCreationDisabled")
    boolean workspaceCreationDisabled;
    @JsonProperty("isGoogleEnabled")
    boolean googleEnabled;
    @JsonProperty("isGithubEnabled")
    boolean githubEnabled;
    @JsonProperty("isGitlabEnabled")
    boolean gitlabEnabled;
    @JsonProperty("isMagicLoginEnabled")
    boolean magicLoginEnabled;
    @JsonProperty("isEmailPasswordEnabled")
    boolean emailPasswordEnabled;

    String githubAppName;

    String posthogApiKey;
    String posthogHost;

    @JsonProperty("isUnsplashConfigured")
    boolean unsplashConfigured;
    double fileSizeLimit;
    @JsonProperty("isSmtpConfigured")
    boolean smtpConfigured;
    @JsonProperty("isIntercomEnabled")
    boolean intercomEnabled;
    String intercomAppId;

    String adminBaseUrl;
    String appBaseUrl;
    String instanceChangelogUrl;
}
