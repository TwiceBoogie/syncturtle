package com.syncturtle.services.email.configurations.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class SyncturtleConfig {
    private Kafka kafka;
    private Email email;
    private Clients clients;

    @Getter
    @Setter
    public static class Kafka {
        private boolean enabled;
        private String consumerGroup;
    }

    @Getter
    @Setter
    public static class Email {
        private String cacheName;
        private Duration cacheTtl;
        private Topics topics;
        private Sender sender;
    }

    @Getter
    @Setter
    public static class Topics {
        private String emailToSend;
        private String emailConfigChanged;
    }

    @Getter
    @Setter
    public static class Sender {
        private int connectionTimeoutMs;
        private int timeoutMs;
        private int writeTimeoutMs;
    }

    @Getter
    @Setter
    public static class Clients {
        private InstanceService instanceService;
    }

    @Getter
    @Setter
    public static class InstanceService {
        private String baseUrl;
        private Duration connectTimeout;
        private Duration readTimeout;
    }
}
