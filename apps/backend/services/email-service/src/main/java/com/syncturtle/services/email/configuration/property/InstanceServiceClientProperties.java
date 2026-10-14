package com.syncturtle.services.email.configuration.property;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.clients.instance-service", ignoreUnknownFields = false)
public final class InstanceServiceClientProperties {

    private final URI baseUrl;

    public InstanceServiceClientProperties(URI baseUrl) {
        this.baseUrl = normalizeRequiredOrigin(baseUrl, "app.clients.instance-service.base-url");
    }

    private static URI normalizeRequiredOrigin(URI uri, String propertyName) {
        Assert.notNull(uri, propertyName + " is required");
        Assert.isTrue(uri.isAbsolute(), propertyName + " must be an absolute URI");

        String scheme = uri.getScheme();

        Assert.isTrue("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme),
                propertyName + " must use http or https");
        Assert.isTrue(StringUtils.hasText(uri.getHost()), propertyName + " must include a host");
        Assert.isNull(uri.getQuery(), propertyName + " must not include a query string");
        Assert.isNull(uri.getFragment(), propertyName + " must not include a fragment");

        String path = uri.getPath();

        Assert.isTrue(!StringUtils.hasText(path) || "/".equals(path),
                propertyName + " must be an origin only; configure endpoint paths separately");

        return trimTrailingSlash(uri);
    }

    private static URI trimTrailingSlash(URI uri) {
        String value = uri.toString();

        while (value.endsWith("/") && value.length() > 1) {
            value = value.substring(0, value.length() - 1);
        }

        return URI.create(value);
    }

}