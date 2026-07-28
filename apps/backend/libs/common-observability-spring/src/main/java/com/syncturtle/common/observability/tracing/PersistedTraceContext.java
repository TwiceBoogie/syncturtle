package com.syncturtle.common.observability.tracing;

import org.springframework.util.StringUtils;

public record PersistedTraceContext(
        String traceparent,
        String tracestate) {

    public PersistedTraceContext {
        traceparent = normalize(traceparent);
        tracestate = normalize(tracestate);
    }

    public boolean isPresent() {
        return traceparent != null;
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

}
