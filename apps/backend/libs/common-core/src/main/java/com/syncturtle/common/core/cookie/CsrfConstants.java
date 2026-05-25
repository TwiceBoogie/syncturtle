package com.syncturtle.common.core.cookie;

public class CsrfConstants {
    private CsrfConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    public static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    public static final String CSRF_FORM_FIELD_NAME = "csrfmiddlewaretoken";
}
