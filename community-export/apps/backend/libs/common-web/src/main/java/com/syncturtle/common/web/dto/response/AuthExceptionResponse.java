package com.syncturtle.common.web.dto.response;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class AuthExceptionResponse {
    private final int errorCode;
    private final String errorMessage;
    private final Map<String, Object> payload;
}
