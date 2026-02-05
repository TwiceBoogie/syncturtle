package com.syncturtle.platform.services.instance.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public final class InstanceInfoResponse implements InstanceInfo {

    private InstanceResponse instance;
    private InstanceConfigResponse config;

    @Data
    public static class InstanceConfigResponse {
        private boolean enableSignup;
        @JsonProperty("isWorkspaceCreationDisabled")
        private boolean workspaceCreationDisabled;
        @JsonProperty("isGoogleEnabled")
        private boolean googleEnabled;
        @JsonProperty("isGithubEnabled")
        private boolean githubEnabled;
        @JsonProperty("isGitlabEnabled")
        private boolean gitlabEnabled;
        @JsonProperty("isMagicLoginEnabled")
        private boolean magicLoginEnabled;
        @JsonProperty("isEmailPasswordEnabled")
        private boolean emailPasswordEnabled;
        private String githubAppName;
        private String posthogApiKey;
        private String posthogHost;
        private int fileSizeLimit;
        @JsonProperty("isSmtpConfigured")
        private boolean smtpConfigured;
        private String appBaseUrl;
        private String adminBaseUrl;
        @JsonProperty("isIntercomEnabled")
        private boolean intercomEnabled;
        private String intercomAppId;
        private String instanceChangelogUrl;
    }
}
