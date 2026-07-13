package com.syncturtle.common.contracts.api.error;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DebugErrorDetails {
    String exception;
    String exceptionMessage;
    String cause;
    @Singular("stackTraceLine")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<String> stackTrace;
}
