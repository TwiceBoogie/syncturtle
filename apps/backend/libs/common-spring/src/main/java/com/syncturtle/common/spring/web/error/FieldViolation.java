package com.syncturtle.common.spring.web.error;

import lombok.Getter;

@Getter
public final class FieldViolation {
    private final String field;
    private final String message;

    public FieldViolation(String field, String message) {
        this.field = field;
        this.message = message;
    }
}
