package com.syncturtle.common.spring.cache.response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

public final class ResponseCacheKeyBuilder {

    /**
     * Canonicalize request into a stable string -> {uri}?{sortedQuery}
     * We use request.getParameterMap() to avoid raw query order issues
     * 
     * @param request a HttpServletRequest
     * @return
     */
    public String canonicalVariantInput(HttpServletRequest request) {
        String uri = request.getRequestURI();

        Map<String, String[]> params = request.getParameterMap();
        if (params == null || params.isEmpty()) {
            return uri;
        }

        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder queryString = new StringBuilder();
        for (String k : keys) {
            String[] values = params.get(k);
            if (values == null) {
                continue;
            }
            List<String> vs = new ArrayList<>(Arrays.asList(values));
            vs.sort(Comparator.naturalOrder());

            for (String v : vs) {
                if (queryString.length() > 0) {
                    queryString.append('&');
                }
                queryString.append(urlEncode(k)).append('=').append(urlEncode(v));
            }
        }

        return uri + "?" + queryString;
    }

    public String variantHashHex(String canonicalVariantInput, int hashBytes) {
        byte[] digest = sha256(canonicalVariantInput);
        int n = Math.min(hashBytes, digest.length);
        return toHex(digest, n);
    }

    public String versionKey(String keyPrefix, String service, String group) {
        // st:resp:{service}:ver:{group}
        return keyPrefix + service + ":ver:" + group;
    }

    public String responseKey(String keyPrefix, String service, String group, String version, String variantHashHex,
            String workspaceScope, String userScope) {
        // st:resp:{service}:{group}:v{N}:h:{hash}[:w:{id}][:u:{id}]
        StringBuilder sb = new StringBuilder(128);
        sb.append(keyPrefix)
                .append(service)
                .append(':')
                .append(group)
                .append(':')
                .append("v")
                .append(version)
                .append(':')
                .append("h:")
                .append(variantHashHex);

        if (!workspaceScope.isBlank()) {
            sb.append(':').append(workspaceScope);
        }

        if (!userScope.isBlank()) {
            sb.append(':').append(userScope);
        }

        return sb.toString();
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
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
