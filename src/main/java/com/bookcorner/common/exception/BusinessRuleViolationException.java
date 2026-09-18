package com.bookcorner.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an operation violates a domain invariant, state machine rule,
 * or core e-commerce business rule.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BusinessRuleViolationException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessRuleViolationException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessRuleViolationException(String errorCode, String message) {
        super(message, errorCode, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public BusinessRuleViolationException(String errorCode, String message, Throwable cause) {
        super(message, cause, errorCode, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
