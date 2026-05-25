package com.syncturtle.common.web.dto.response;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public final class AuthExceptionResponse {
    private final int errorCode;
    private final String errorMessage;
    private final Map<String, Object> payload;

    @JsonCreator
    public AuthExceptionResponse(
            @JsonProperty("errorCode") int errorCode,
            @JsonProperty("errorMessage") String errorMessage,
            @JsonProperty("payload") Map<String, Object> payload) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.payload = payload;
    }
}
