package com.syncturtle.common.web.error;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

public final class TraceIds {

    private TraceIds() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String resolve() {
        String traceId = MDC.get("traceId");

        return StringUtils.hasText(traceId) ? traceId.trim() : null;
    }

}
