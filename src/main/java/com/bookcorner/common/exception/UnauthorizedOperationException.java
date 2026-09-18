package com.bookcorner.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an authenticated caller attempts to access or mutate a resource they do not own
 * or do not have sufficient role permissions to perform.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedOperationException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "FORBIDDEN";

    public UnauthorizedOperationException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.FORBIDDEN);
    }

    public UnauthorizedOperationException(String message, Throwable cause) {
        super(message, cause, DEFAULT_ERROR_CODE, HttpStatus.FORBIDDEN);
    }
}
