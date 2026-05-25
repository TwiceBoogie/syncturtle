package com.syncturtle.common.web.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.web.errors")
public class ErrorResponseProperties {
    /**
     * Include request path in error message.
     */
    private final boolean includePath;
    /**
     * Include trace_id, request_id, and correlation_id in error response.
     */
    private final boolean includeRequestIds;
    /**
     * Include debug object. Keep false in production.
     */
    private final boolean includeDebug;
    /**
     * Include stack trace lines inside debug.
     * Keep false in production unless troubleshooiting in private environment.
     */
    private final boolean includeStackTrace;
    /**
     * Max stack trace lines returned when incldueStackTrace=true
     */
    private final int maxStackTraceLines;

    public ErrorResponseProperties(
            @DefaultValue("true") boolean includePath,
            @DefaultValue("true") boolean includeRequestIds,
            @DefaultValue("false") boolean includeDebug,
            @DefaultValue("false") boolean includeStackTrace,
            @DefaultValue("30") int maxStackTraceLines) {
        this.includePath = includePath;
        this.includeRequestIds = includeRequestIds;
        this.includeDebug = includeDebug;
        this.includeStackTrace = includeStackTrace;
        this.maxStackTraceLines = requirePositive(maxStackTraceLines, "max-stack-trace-lines");

        if (includeStackTrace && !includeDebug) {
            throw new IllegalArgumentException(
                    "app.web.errors.include-stack-trace requires include-debug=true");
        }
    }

    private static int requirePositive(int value, String propertyName) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "app.web.errors." + propertyName + " must be greater than 0");
        }

        return value;
    }
}
