package com.syncturtle.common.spring.web.error;

import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.syncturtle.common.core.error.ErrorCode;
import com.syncturtle.common.core.exceptions.SyncturtleException;

public abstract class SyncturtleServiceException extends SyncturtleException
        implements PublicMessageAwareException, HttpStatusAwareException {

    private final HttpStatus status;
    private final String publicMessage;

    protected SyncturtleServiceException(
            ErrorCode errorCode,
            HttpStatus status,
            String publicMessage,
            Map<String, Object> payload,
            Throwable cause) {
        super(errorCode, payload, cause);
        this.status = Objects.requireNonNull(status, "status is required");
        this.publicMessage = Objects.requireNonNull(publicMessage, "publicMessage is required");
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getPublicMessage() {
        return publicMessage;
    }

}
