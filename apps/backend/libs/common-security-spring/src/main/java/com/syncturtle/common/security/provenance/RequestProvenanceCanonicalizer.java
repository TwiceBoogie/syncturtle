package com.syncturtle.common.security.provenance;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

import com.syncturtle.common.core.header.GatewayHeaders;
import com.syncturtle.common.security.exception.RequestProvenanceException;

public final class RequestProvenanceCanonicalizer {

    private static final Pattern HTTP_METHOD = Pattern.compile("[A-Z]{3,12}");
    private static final Pattern RAW_PATH = Pattern.compile("/[A-Za-z0-9\\-._~%!$&'()*+,;=:@/]*");
    private static final Base64.Encoder B64URL = Base64.getUrlEncoder().withoutPadding();

    private static final String ACTOR_HEADER = GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID.toLowerCase(Locale.ROOT);
    private static final List<String> PROTECTED_HEADERS = sortedHeaders(List.of(
            GatewayHeaders.HDR_AUTH_USER_ID,
            GatewayHeaders.HDR_AUTH_SESSION_ID,
            GatewayHeaders.HDR_AUTH_INSTANCE_ID,
            GatewayHeaders.HDR_AUTH_ROLES,
            GatewayHeaders.HDR_AUTH_USER_AUTH_VERSION,
            GatewayHeaders.HDR_AUTH_ADMIN_SESSION_VERSION,
            GatewayHeaders.HDR_AUTH_ISSUER,
            GatewayHeaders.HDR_AUTH_WORKSPACE_ID,
            GatewayHeaders.HDR_CLIENT_IP,
            GatewayHeaders.HDR_CLIENT_UA,
            GatewayHeaders.HDR_CLIENT_DEVICE_ID,
            GatewayHeaders.HDR_CLIENT_LOCALE,
            GatewayHeaders.HDR_PREAUTH_TRANSACTION_BINDING,
            GatewayHeaders.HDR_INTERNAL_CSRF_SESSION_ID,
            GatewayHeaders.HDR_INTERNAL_LOGIN_CONTEXT,
            GatewayHeaders.HDR_INTERNAL_LOGOUT_CONTEXT,
            GatewayHeaders.HDR_INTERNAL_ACTOR_USER_ID));

    public String canonicalMethod(String method) {
        if (method == null || method.isBlank()) {
            throw invalid("HTTP method is required");
        }

        String canonical = method.trim().toUpperCase(Locale.ROOT);
        if (!HTTP_METHOD.matcher(canonical).matches()) {
            throw invalid("HTTP method is invalid");
        }

        return canonical;
    }

    public String canonicalRawPath(String rawPath) {
        if (rawPath == null
                || rawPath.length() > RequestProvenanceProtocol.MAX_RAW_PATH_CHARACTERS
                || !RAW_PATH.matcher(rawPath).matches()) {
            throw invalid("raw request path is invalid");
        }

        rejectInvalidPercentEncoding(rawPath);

        String lower = rawPath.toLowerCase(Locale.ROOT);
        if (lower.contains("%2e") || lower.contains("%5c") || rawPath.contains("//")) {
            throw invalid("ambiguous raw request path is not allowed");
        }

        return rawPath;
    }

    public String canonicalRawQuery(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }

        if (rawQuery.length() > RequestProvenanceProtocol.MAX_RAW_QUERY_CHARACTERS
                || rawQuery.indexOf('#') >= 0) {
            throw invalid("raw query is invalid");
        }

        rejectControlCharacters(rawQuery, "raw query");
        return rawQuery;
    }

    public String digestRawQuery(String rawQuery) {
        return sha256Base64Url(canonicalRawQuery(rawQuery).getBytes(StandardCharsets.UTF_8));
    }

    public String digestProtectedHeaders(RequestHeaderSource source) {
        Assert.notNull(source, "request header source is required");

        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);

            for (String headerName : PROTECTED_HEADERS) {
                byte[] nameBytes = headerName.getBytes(StandardCharsets.UTF_8);
                output.writeShort(nameBytes.length);
                output.write(nameBytes);

                List<String> values = safeValues(source.values(headerName));
                if (values.isEmpty()) {
                    output.writeInt(-1);
                    continue;
                }

                if (values.size() != 1) {
                    throw invalid("duplicate protected header");
                }

                byte[] valueBytes = canonicalHeaderValue(values.getFirst()).getBytes(StandardCharsets.UTF_8);
                output.writeInt(valueBytes.length);
                output.write(valueBytes);
            }

            output.flush();
            return sha256Base64Url(bytes.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to canonicalize protected headers", exception);
        }
    }

    public boolean hasAnyProtectedHeader(RequestHeaderSource source) {
        Assert.notNull(source, "request header source is required");

        for (String headerName : PROTECTED_HEADERS) {
            if (!safeValues(source.values(headerName)).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasNonActorProtectedHeader(RequestHeaderSource source) {
        Assert.notNull(source, "request header source is required");

        for (String headerName : PROTECTED_HEADERS) {
            if (ACTOR_HEADER.equals(headerName)) {
                continue;
            }

            if (!safeValues(source.values(headerName)).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public List<String> protectedHeaderNames() {
        return PROTECTED_HEADERS;
    }

    public static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }

        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    private static String canonicalHeaderValue(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw invalid("protected header value is invalid");
        }

        rejectControlCharacters(value, "protected header value");

        return value;
    }

    private static void rejectInvalidPercentEncoding(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '%') {
                continue;
            }

            if (i + 2 >= value.length() || !isHex(value.charAt(i + 1)) || !isHex(value.charAt(i + 2))) {
                throw invalid("raw request path contains invalid percent encoding");
            }

            i += 2;
        }
    }

    private static boolean isHex(char value) {
        return (value >= '0' && value <= '9') || (value >= 'A' && value <= 'F') || (value >= 'a' && value <= 'f');
    }

    private static void rejectControlCharacters(String value, String label) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character <= 31 || character == 127) {
                throw invalid(label + " contains a control character");
            }
        }
    }

    private static List<String> safeValues(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static List<String> sortedHeaders(List<String> values) {
        List<String> headers = new ArrayList<>(values.size());
        for (String value : values) {
            headers.add(value.toLowerCase(Locale.ROOT));
        }

        headers.sort(String::compareTo);

        return List.copyOf(headers);
    }

    private static String sha256Base64Url(byte[] value) {
        try {
            return B64URL.encodeToString(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static RequestProvenanceException invalid(String message) {
        return new RequestProvenanceException(message);
    }

}
