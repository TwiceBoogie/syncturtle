package com.syncturtle.common.core.exceptions;

import java.util.Map;

import com.syncturtle.common.core.error.ErrorCode;

public abstract class SyncturtleServiceException extends SyncturtleException {

    protected SyncturtleServiceException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected SyncturtleServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    protected SyncturtleServiceException(ErrorCode errorCode, Map<String, Object> payload) {
        super(errorCode, payload);
    }

    protected SyncturtleServiceException(ErrorCode errorCode, Map<String, Object> payload, Throwable cause) {
        super(errorCode, payload, cause);
    }

    protected SyncturtleServiceException(ErrorCode errorCode, int httpStatusCode, String publicMessage,
            Map<String, Object> payload, Throwable cause) {
        super(errorCode, httpStatusCode, publicMessage, payload, cause);
    }

}
