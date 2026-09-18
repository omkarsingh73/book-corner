package com.bookcorner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when an optimistic concurrency lock collision occurs due to concurrent entity modifications.
 */
@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class OptimisticLockingException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "CONCURRENT_MODIFICATION_CONFLICT";

    private final String entityName;
    private final UUID entityId;
    private final Long attemptedVersion;

    public OptimisticLockingException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.CONFLICT);
        this.entityName = null;
        this.entityId = null;
        this.attemptedVersion = null;
    }

    public OptimisticLockingException(String entityName, UUID entityId, Long attemptedVersion) {
        super(String.format("Entity '%s' [ID: %s] has been modified by another transaction (version %d). Please refresh and retry.",
                entityName, entityId, attemptedVersion), DEFAULT_ERROR_CODE, HttpStatus.CONFLICT);
        this.entityName = entityName;
        this.entityId = entityId;
        this.attemptedVersion = attemptedVersion;
    }
}
