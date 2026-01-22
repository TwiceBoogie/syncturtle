package com.syncturtle.common.spring.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.frontend")
public class HostRoutingProperties {
    private String webUrl;
    private String appBaseUrl;
    private String adminBaseUrl;
    private String spaceBaseUrl;
    private String adminBasePath = "/god-mode/";
    private String spaceBasePath = "/spaces/";
}
