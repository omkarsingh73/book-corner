package com.bookcorner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown on unique constraint violations (e.g. duplicate email address, existing review, duplicated ISBN).
 */
@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "DUPLICATE_RESOURCE";

    private final String resourceName;
    private final String field;
    private final Object conflictingValue;

    public DuplicateResourceException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.CONFLICT);
        this.resourceName = null;
        this.field = null;
        this.conflictingValue = null;
    }

    public DuplicateResourceException(String resourceName, String field, Object conflictingValue) {
        super(String.format("%s with %s '%s' already exists.", resourceName, field, conflictingValue),
                DEFAULT_ERROR_CODE, HttpStatus.CONFLICT);
        this.resourceName = resourceName;
        this.field = field;
        this.conflictingValue = conflictingValue;
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause, DEFAULT_ERROR_CODE, HttpStatus.CONFLICT);
        this.resourceName = null;
        this.field = null;
        this.conflictingValue = null;
    }
}
