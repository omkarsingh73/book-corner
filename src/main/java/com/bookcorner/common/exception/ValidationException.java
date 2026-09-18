package com.bookcorner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Thrown when custom domain or business validation fails.
 * Collects field-level validation errors.
 */
@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "VALIDATION_FAILED";

    private final List<ProblemDetails.InvalidParam> validationErrors;

    public ValidationException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.BAD_REQUEST);
        this.validationErrors = new ArrayList<>();
    }

    public ValidationException(String message, List<ProblemDetails.InvalidParam> validationErrors) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.BAD_REQUEST);
        this.validationErrors = validationErrors != null ? validationErrors : new ArrayList<>();
    }

    public ValidationException(String field, String reason, Object rejectedValue) {
        super(String.format("Validation failed on field '%s': %s", field, reason),
                DEFAULT_ERROR_CODE, HttpStatus.BAD_REQUEST);
        this.validationErrors = List.of(ProblemDetails.InvalidParam.of(field, reason, rejectedValue));
    }
}
