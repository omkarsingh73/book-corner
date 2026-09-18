package com.bookcorner.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a supplied JWT access or refresh token has expired.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenExpiredException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "TOKEN_EXPIRED";

    public TokenExpiredException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.UNAUTHORIZED);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause, DEFAULT_ERROR_CODE, HttpStatus.UNAUTHORIZED);
    }
}
