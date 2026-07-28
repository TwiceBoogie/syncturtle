package com.syncturtle.services.email.support.fixture;

import com.syncturtle.common.contracts.email.config.EmailRuntimeSecretConfigResponse;
import com.syncturtle.services.email.service.runtime.EmailRuntimeConfigSnapshot;

public final class EmailRuntimeConfigFixtures {

    public static final String SMTP_HOST = "smtp.example.com";
    public static final int SMTP_PORT = 2525;
    public static final String SMTP_USERNAME = "smtp-user";
    public static final String SMTP_PASSWORD = " smtp-secret ";
    public static final String FROM = "no-reply@syncturtle.com";
    public static final long VERSION = 12L;

    private EmailRuntimeConfigFixtures() {
    }

    public static EmailRuntimeConfigSnapshot completeRuntimeConfig() {
        return completeRuntimeConfig(SMTP_HOST, SMTP_PORT);
    }

    public static EmailRuntimeConfigSnapshot completeRuntimeConfig(String host, int port) {
        return EmailRuntimeConfigSnapshot.builder()
                .enabled(true)
                .host(host)
                .port(port)
                .username(SMTP_USERNAME)
                .password(SMTP_PASSWORD)
                .from(FROM)
                .useTls(false)
                .useSsl(false)
                .version(VERSION)
                .build();
    }

    public static EmailRuntimeConfigSnapshot unauthenticatedRuntimeConfig(String host, int port) {
        return EmailRuntimeConfigSnapshot.builder()
                .enabled(true)
                .host(host)
                .port(port)
                .from(FROM)
                .useTls(false)
                .useSsl(false)
                .version(VERSION)
                .build();
    }

    public static EmailRuntimeConfigSnapshot disabledRuntimeConfig() {
        return EmailRuntimeConfigSnapshot.builder()
                .enabled(false)
                .version(VERSION)
                .build();
    }

    public static EmailRuntimeConfigSnapshot incompleteRuntimeConfig() {
        return EmailRuntimeConfigSnapshot.builder()
                .enabled(true)
                .version(VERSION)
                .build();
    }

    public static EmailRuntimeSecretConfigResponse runtimeConfigResponse() {
        return EmailRuntimeSecretConfigResponse.builder()
                .enabled(true)
                .host(SMTP_HOST)
                .port(SMTP_PORT)
                .username(SMTP_USERNAME)
                .password(SMTP_PASSWORD)
                .from(FROM)
                .useTls(false)
                .useSsl(false)
                .version(VERSION)
                .build();
    }
}
