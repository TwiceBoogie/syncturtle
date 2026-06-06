package com.syncturtle.common.web.url;

import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.syncturtle.common.web.properties.PublicUrlProperties;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class PublicUrlResolver {

    private final PublicUrlProperties properties;

    public String api(String path) {
        return url(PublicUrlTarget.API, path);
    }

    public URI apiUri(String path) {
        return URI.create(api(path));
    }

    public String apiWithQuery(String path, Map<String, ?> queryparams) {
        return urlWithQuery(PublicUrlTarget.API, path, queryparams);
    }

    public String userApp(String path) {
        return url(PublicUrlTarget.USER_APP, path);
    }

    public URI userAppUri(String path) {
        return URI.create(userApp(path));
    }

    public String userAppWithQuery(String path, Map<String, ?> queryParams) {
        return urlWithQuery(PublicUrlTarget.USER_APP, path, queryParams);
    }

    public String admin(String path) {
        return url(PublicUrlTarget.ADMIN, path);
    }

    public URI adminUri(String path) {
        return URI.create(admin(path));
    }

    public String adminWithQuery(String path, Map<String, ?> queryParams) {
        return urlWithQuery(PublicUrlTarget.ADMIN, path, queryParams);
    }

    public String url(PublicUrlTarget target, String path) {
        return urlWithQuery(target, path, Map.of());
    }

    public URI uri(PublicUrlTarget target, String path) {
        return URI.create(url(target, path));
    }

    public String urlWithQuery(
            PublicUrlTarget target,
            String path,
            Map<String, ?> queryParams) {
        RouteParts route = routeParts(target);
        String joinedPath = joinPaths(route.basePath(), path);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(route.origin());

        if (StringUtils.hasText(joinedPath)) {
            builder.path(joinedPath);
        }

        builder.queryParams(toQueryParams(queryParams));

        return builder.build()
                .encode()
                .toUriString();
    }

    public URI uriWithQuery(
            PublicUrlTarget target,
            String path,
            Map<String, ?> queryParams) {
        return URI.create(urlWithQuery(target, path, queryParams));
    }

    private RouteParts routeParts(PublicUrlTarget target) {
        return switch (target) {
            case API -> new RouteParts(properties.getApi().getOrigin(), properties.getApi().getBasePath());
            case USER_APP -> new RouteParts(properties.getUserApp().getOrigin(), properties.getUserApp().getBasePath());
            case ADMIN -> new RouteParts(properties.getAdmin().getOrigin(), properties.getAdmin().getBasePath());
        };
    }

    private static String joinPaths(String basePath, String path) {
        String normalizedBasePath = normalizeBasePathForJoin(basePath);
        String normalizedRelativePath = normalizeRelativePath(path);

        if (!StringUtils.hasText(normalizedBasePath) && !StringUtils.hasText(normalizedRelativePath)) {
            return "";
        }

        if (!StringUtils.hasText(normalizedRelativePath)) {
            return normalizedBasePath;
        }

        if (!StringUtils.hasText(normalizedBasePath) || "/".equals(normalizedRelativePath)) {
            return "/" + normalizedRelativePath;
        }

        return normalizedBasePath + normalizedRelativePath;
    }

    private static String normalizeBasePathForJoin(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }

        String path = raw.trim();

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (!path.endsWith("/")) {
            path = path + "/";
        }

        return path;
    }

    private static String normalizeRelativePath(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }

        String path = raw.trim();

        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        return path;
    }

    private static MultiValueMap<String, String> toQueryParams(Map<String, ?> raw) {
        LinkedMultiValueMap<String, String> out = new LinkedMultiValueMap<>();

        if (raw == null || raw.isEmpty()) {
            return out;
        }

        for (Entry<String, ?> entry : raw.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (!StringUtils.hasText(key) || value == null) {
                continue;
            }

            if (value instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (item != null) {
                        out.add(key, normalizeQueryValue(item));
                    }
                }
            } else {
                out.add(key, normalizeQueryValue(value));
            }
        }

        return out;
    }

    private static String normalizeQueryValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool ? "true" : "false";
        }

        return String.valueOf(value);
    }

    private static final class RouteParts {
        private final String origin;
        private final String basePath;

        private RouteParts(String origin, String basePath) {
            this.origin = origin;
            this.basePath = basePath;
        }

        private String origin() {
            return origin;
        }

        private String basePath() {
            return basePath;
        }
    }

}
