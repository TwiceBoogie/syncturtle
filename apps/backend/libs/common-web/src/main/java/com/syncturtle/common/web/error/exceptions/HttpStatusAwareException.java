package com.syncturtle.common.web.error.exceptions;

import org.springframework.http.HttpStatus;

public interface HttpStatusAwareException {
    HttpStatus getStatus();
}
