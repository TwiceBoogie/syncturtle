package com.syncturtle.common.core.exceptions;

public final class SlugAlreadyExistsException extends RuntimeException {

    public SlugAlreadyExistsException(String message) {
        super(message);
    }

    public SlugAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

}
