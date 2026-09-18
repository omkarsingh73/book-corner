package com.bookcorner.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a supplied JWT token has an invalid signature, is malformed, or contains invalid claims.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidTokenException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "INVALID_TOKEN";

    public InvalidTokenException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.UNAUTHORIZED);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause, DEFAULT_ERROR_CODE, HttpStatus.UNAUTHORIZED);
    }
}
