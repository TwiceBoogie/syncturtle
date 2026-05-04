package com.syncturtle.common.spring.web.error;

import org.springframework.http.HttpStatus;

public interface HttpStatusAwareException {
    HttpStatus getStatus();
}
