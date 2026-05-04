package com.syncturtle.common.spring.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.base-urls")
public class HostRoutingProperties {
    private final String webUrl;
    private final String appBaseUrl;
    private final String adminBaseUrl;
    private final String spaceBaseUrl;
    private final String adminBasePath = "/god-mode/";
    private final String spaceBasePath = "/spaces/";
}
