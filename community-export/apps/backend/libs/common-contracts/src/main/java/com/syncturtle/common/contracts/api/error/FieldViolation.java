package com.syncturtle.common.contracts.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldViolation {
    String field;
    String errorMessage;
    String message;
}
