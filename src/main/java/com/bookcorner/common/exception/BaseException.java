package com.bookcorner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Abstract foundational base exception for the Book Corner platform.
 * Encapsulates machine-readable error codes, HTTP status mapping,
 * and contextual metadata.
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Instant timestamp;

    protected BaseException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.timestamp = Instant.now();
    }

    protected BaseException(String message, Throwable cause, String errorCode, HttpStatus httpStatus) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.timestamp = Instant.now();
    }

    protected BaseException(String message, HttpStatus httpStatus) {
        this(message, httpStatus.name(), httpStatus);
    }
}
