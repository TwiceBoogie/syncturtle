package com.syncturtle.common.spring.web.url;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.syncturtle.common.spring.properties.HostRoutingProperties;

public class HostUrlBuilder {

    private final HostRoutingProperties props;

    public HostUrlBuilder(HostRoutingProperties props) {
        this.props = props;
    }

    public String baseOrigin() {
        String base = firstNonBlank(props.getWebUrl(), props.getAppBaseUrl());
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException(
                    "No base origin configured. Set app.frontend.web-url or app.frontend.app-base-url");
        }
        return trimTrailingSlash(base);
    }

    public String baseHost(boolean isAdmin, boolean isSpace, boolean isApp) {
        if (isAdmin) {
            return adminHost();
        }
        if (isSpace) {
            return spaceHost();
        }
        if (isApp) {
            return appHost();
        }
        return baseOrigin();
    }

    public String adminHost() {
        String origin = firstNonBlank(props.getAdminBaseUrl(), baseOrigin());
        String path = normalizePath(props.getAdminBasePath(), "/god-mode/");
        return UriComponentsBuilder.fromUriString(trimTrailingSlash(origin)).path(path).build(true).toUriString();
    }

    public String spaceHost() {
        String origin = firstNonBlank(props.getSpaceBaseUrl(), baseOrigin());
        String path = normalizePath(props.getSpaceBasePath(), "/spaces/");
        return UriComponentsBuilder.fromUriString(trimTrailingSlash(origin)).path(path).build(true).toUriString();
    }

    public String appHost() {
        String origin = firstNonBlank(props.getAppBaseUrl(), baseOrigin());
        return trimTrailingSlash(origin);
    }

    public String buildRedirectUriWithQuery(String baseHost, Map<String, ?> queryParams) {
        MultiValueMap<String, String> encoded = toQueryParams(queryParams);

        return UriComponentsBuilder.fromUriString(baseHost).queryParams(encoded).build().toUriString();
    }

    public String buildAdminRedirectUriWithErrors(Map<String, ?> errors) {
        return buildRedirectUriWithQuery(adminHost(), errors);
    }

    private static MultiValueMap<String, String> toQueryParams(Map<String, ?> raw) {
        LinkedMultiValueMap<String, String> out = new LinkedMultiValueMap<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }

        for (Entry<String, ?> entry : raw.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue;
            }

            if (value instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item != null) {
                        out.add(key, String.valueOf(item));
                    }
                }
            } else {
                out.add(key, String.valueOf(value));
            }
        }
        return out;
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a;
        }
        if (StringUtils.hasText(b)) {
            return b;
        }
        return null;
    }

    private static String trimTrailingSlash(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String normalizePath(String raw, String defaultPath) {
        String path = StringUtils.hasText(raw) ? raw : defaultPath;

        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }

        return path;
    }
}
