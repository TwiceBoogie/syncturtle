package com.syncturtle.common.contracts.api.error;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    @Builder.Default
    boolean ok = false;

    Integer code;
    String key;
    String message;
    String traceId;
    String requestId;
    String correlationId;
    String path;
    @Builder.Default
    Instant timestamp = Instant.now();
    @Singular
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<FieldViolation> fields;
    /**
     * Safe extra data for clients.
     * 
     * <p>
     * For auth redirects this maps to query params.
     * For JSON this can carry safe values like email, first_name, last_name, etc.
     * 
     */
    @Singular("metaEntry")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Map<String, Object> meta;
    /**
     * Development-only details.
     * 
     * <p>
     * Must be null in production
     */
    DebugErrorDetails debug;
}
