package com.syncturtle.services.instance.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app")
public final class InstanceServiceProperties {
    private final String name;
    private final String edition;
    private final boolean test;

    private final Telemetry telemetry;
    private final boolean skipEnvVar;

    private final ConfigKeys configKeys;
    private final BaseUrls baseUrls;
    private final Kafka kafka;

    private final Security security;

    @Getter
    @RequiredArgsConstructor
    public static final class Telemetry {
        private final boolean enabled;
        private final boolean supportRequired;
    }

    @Getter
    @RequiredArgsConstructor
    public static final class ConfigKeys {
        private final int enableSignup;
        private final int enableEmailPassword;
        private final int disableWorkspaceCreation;

        private final int enableSmtp;
        private final String emailHost;
        private final String emailHostUser;
        private final String emailHostPassword;
        private final int emailPort;
        private final String emailFrom;
        private final int emailUseTls;
        private final int emailUseSsl;

        private final int enableMagicLinkLogin;

        private final String googleClientId;
        private final String googleClientSecret;

        private final String githubClientId;
        private final String githubClientSecret;

        private final String gitlabHost;
        private final String gitlabClientId;
        private final String gitlabClientSecret;

        private final String intercomAppId;

        private final String posthogApiKey;
        private final String posthogHost;
    }

    @Getter
    @RequiredArgsConstructor
    public static final class BaseUrls {
        private final String admin;
        private final String app;
    }

    @Getter
    @RequiredArgsConstructor
    public static final class Security {
        private final Encryption encryption;
    }

    @Getter
    @RequiredArgsConstructor
    public static final class Encryption {
        private final String secretKey;
    }

    @Getter
    @RequiredArgsConstructor
    public static final class Kafka {
        private final boolean enabled;
    }
}
