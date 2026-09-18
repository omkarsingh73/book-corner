package com.bookcorner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an entity or resource is not found in the persistence store.
 */
@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "RESOURCE_NOT_FOUND";

    private final String resourceName;
    private final Object identifier;

    public ResourceNotFoundException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.NOT_FOUND);
        this.resourceName = "Resource";
        this.identifier = null;
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s not found with identifier: '%s'", resourceName, identifier),
                DEFAULT_ERROR_CODE, HttpStatus.NOT_FOUND);
        this.resourceName = resourceName;
        this.identifier = identifier;
    }

    public ResourceNotFoundException(String resourceName, Object identifier, Throwable cause) {
        super(String.format("%s not found with identifier: '%s'", resourceName, identifier),
                cause, DEFAULT_ERROR_CODE, HttpStatus.NOT_FOUND);
        this.resourceName = resourceName;
        this.identifier = identifier;
    }
}
