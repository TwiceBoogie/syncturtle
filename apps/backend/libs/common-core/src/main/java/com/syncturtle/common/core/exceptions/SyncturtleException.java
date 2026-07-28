package com.syncturtle.common.core.exceptions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.syncturtle.common.core.error.ErrorCode;

public class SyncturtleException extends RuntimeException {

    private final ErrorCode errorCode;
    private final int httpStatusCode;
    private final String publicMessage;
    private final Map<String, Object> payload;

    public SyncturtleException(ErrorCode errorCode) {
        this(errorCode, errorCode.getHttpStatusCode(), errorCode.getPublicMessage(), Map.of(), null);
    }

    public SyncturtleException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, errorCode.getHttpStatusCode(), errorCode.getPublicMessage(), Map.of(), cause);
    }

    public SyncturtleException(ErrorCode errorCode, Map<String, Object> payload) {
        this(errorCode, errorCode.getHttpStatusCode(), errorCode.getPublicMessage(), payload, null);
    }

    public SyncturtleException(ErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        this(errorCode, errorCode.getHttpStatusCode(), errorCode.getPublicMessage(), payload, cause);
    }

    protected SyncturtleException(ErrorCode errorCode, int httpStatusCode, String publicMessage,
            Map<String, Object> payload, Throwable cause) {
        super(Objects.requireNonNull(errorCode, "errorCode is required").getKey(), cause);

        this.errorCode = errorCode;
        this.httpStatusCode = httpStatusCode;
        this.publicMessage = requirePublicMessage(publicMessage);
        this.payload = copyPayload(payload);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getCode() {
        return errorCode.getCode();
    }

    public String getErrorKey() {
        return errorCode.getKey();
    }

    @Deprecated
    public String getErrorMessage() {
        return errorCode.getKey();
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getPublicMessage() {
        return publicMessage;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    /**
     * Redirect/query-param firendly representation.
     * 
     * <p>
     * prefer using a web-layer RedirectErrorQueryFactory for new code,
     * but this is safe to keep for compatibility.
     */
    public Map<String, Object> getErrorMap() {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error_code", errorCode.getCode());
        error.put("error_key", errorCode.getKey());
        error.putAll(payload);

        return Collections.unmodifiableMap(error);
    }

    protected Map<String, Object> copyPayloadWith(String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(payload);

        if (value == null) {
            copy.remove(key);
        } else {
            copy.put(key, value);
        }

        return copy;
    }

    private static String requirePublicMessage(String publicMessage) {
        if (publicMessage == null || publicMessage.isBlank()) {
            return "Request failed.";
        }

        return publicMessage;
    }

    private static Map<String, Object> copyPayload(Map<String, Object> payload) {
        Map<String, Object> copy = new LinkedHashMap<>();

        if (payload != null) {
            copy.putAll(payload);
        }

        return Collections.unmodifiableMap(copy);
    }

}
