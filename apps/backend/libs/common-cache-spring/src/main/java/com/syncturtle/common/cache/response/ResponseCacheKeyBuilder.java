package com.syncturtle.common.cache.response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

public final class ResponseCacheKeyBuilder {

    /**
     * Canonicalize request into a stable string -> {uri}?{sortedQuery}
     * We use request.getParameterMap() to avoid raw query order issues
     * 
     * @param request a HttpServletRequest
     * @return
     */
    public String canonicalVariantInput(HttpServletRequest request, String[] varyHeaders) {
        StringBuilder sb = new StringBuilder(256);

        sb.append(request.getMethod())
                .append(' ')
                .append(request.getRequestURI());

        String canonicalQuery = canonicalQueryString(request);
        if (StringUtils.hasText(canonicalQuery)) {
            sb.append('?').append(canonicalQuery);
        }

        String canonicalHeaders = canonicalHeaders(request, varyHeaders);
        if (StringUtils.hasText(canonicalHeaders)) {
            sb.append("#headers[").append(canonicalHeaders).append(']');
        }

        return sb.toString();
    }

    public String variantHashHex(String canonicalVariantInput, int hashBytes) {
        byte[] digest = sha256(canonicalVariantInput);
        int n = Math.min(hashBytes, digest.length);
        return toHex(digest, n);
    }

    /**
     * st:local:rc:{service}:{group}:ver
     */
    public String versionKey(String keyPrefix, String service, String group) {
        return new StringBuilder(128)
                .append(requireSegment(keyPrefix, "keyPrefix"))
                .append(requireSegment(service, "service"))
                .append(':')
                .append(requireSegment(group, "group"))
                .append(":ver")
                .toString();
    }

    /**
     * st:local:rc:{service}:{group}:data:g{generation}[:w:{workspaceId}][:u:{userId}]:h:{hash}
     */
    public String responseKey(String keyPrefix, String service, String group, String generation, String variantHashHex,
            String workspaceScope, String userScope) {
        StringBuilder sb = new StringBuilder(192);

        sb.append(requireSegment(keyPrefix, "keyPrefix"))
                .append(requireSegment(service, "service"))
                .append(':')
                .append(requireSegment(group, "group"))
                .append(":data:g")
                .append(requireGeneration(generation))
                .append(optionalScope(workspaceScope))
                .append(optionalScope(userScope))
                .append(":h:")
                .append(requireSegment(variantHashHex, "variantHashHex"));

        return sb.toString();
    }

    public String userScope(String userId) {
        return "u:" + requireScopeValue(userId, "userId");
    }

    public String workspaceScope(String workspaceId) {
        return "w:" + requireScopeValue(workspaceId, "workspaceId");
    }

    private static String canonicalQueryString(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if (params == null || params.isEmpty()) {
            return "";
        }

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder queryString = new StringBuilder();

        for (String key : keys) {
            String[] values = params.get(key);
            if (values == null) {
                continue;
            }

            List<String> sortedValues = new ArrayList<>(Arrays.asList(values));
            sortedValues.sort(Comparator.naturalOrder());

            for (String value : sortedValues) {
                if (queryString.length() > 0) {
                    queryString.append('&');
                }

                queryString.append(urlEncode(key))
                        .append('=')
                        .append(urlEncode(value));
            }
        }

        return queryString.toString();
    }

    private static String canonicalHeaders(HttpServletRequest request, String[] varyHeaders) {
        if (varyHeaders == null || varyHeaders.length == 0) {
            return "";
        }

        List<String> headerNames = new ArrayList<>();
        for (String headerName : varyHeaders) {
            if (StringUtils.hasText(headerName)) {
                headerNames.add(headerName.trim());
            }
        }

        if (headerNames.isEmpty()) {
            return "";
        }

        headerNames.sort(String.CASE_INSENSITIVE_ORDER);

        StringBuilder sb = new StringBuilder();

        for (String headerName : headerNames) {
            List<String> values = headerValues(request, headerName);
            values.sort(Comparator.naturalOrder());

            if (sb.length() > 0) {
                sb.append('&');
            }

            sb.append(headerName.toLowerCase())
                    .append('=')
                    .append(urlEncode(String.join(",", values)));
        }

        return sb.toString();
    }

    private static List<String> headerValues(HttpServletRequest request, String headerName) {
        Enumeration<String> enumeration = request.getHeaders(headerName);

        if (enumeration == null) {
            return new ArrayList<>();
        }

        List<String> values = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            values.add(enumeration.nextElement());
        }

        return values;
    }

    private static String optionalScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return "";
        }

        return ":" + scope.trim();
    }

    private static String requireGeneration(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("generation is required");
        }

        String trimmed = value.trim();

        if (!trimmed.matches("\\d+")) {
            throw new IllegalArgumentException("generation must be numeric");
        }

        return trimmed;
    }

    private static String requireScopeValue(String value, String name) {
        String segment = requireSegment(value, name);

        if (segment.indexOf(':') >= 0) {
            throw new IllegalArgumentException(name + " must not contain ':'");
        }

        return segment;
    }

    private static String requireSegment(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(name + " is required");
        }

        String trimmed = value.trim();

        if (trimmed.contains(" ")) {
            throw new IllegalArgumentException(name + " must not contain spaces");
        }

        return trimmed;
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static byte[] sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toHex(byte[] bytes, int length) {
        char[] hex = new char[length * 2];
        final char[] alphabet = "0123456789abcdef".toCharArray();
        for (int i = 0; i < length; i++) {
            int b = bytes[i] & 0xFF;
            hex[i * 2] = alphabet[b >>> 4];
            hex[i * 2 + 1] = alphabet[b & 0x0F];
        }
        return new String(hex);
    }
}
