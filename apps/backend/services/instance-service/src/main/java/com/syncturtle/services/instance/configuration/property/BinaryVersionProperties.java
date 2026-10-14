package com.syncturtle.services.instance.configuration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.binary")
public final class BinaryVersionProperties {

    private final String version;

}
