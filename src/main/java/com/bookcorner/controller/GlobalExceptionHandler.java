package com.bookcorner.controller;

import com.bookcorner.common.exception.BusinessRuleViolationException;
import com.bookcorner.common.exception.DuplicateResourceException;
import com.bookcorner.common.exception.InsufficientStockException;
import com.bookcorner.common.exception.InvalidCredentialsException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.common.exception.UnauthorizedOperationException;
import com.bookcorner.dto.common.ErrorEnvelope;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Global REST exception handler mapping application domain exceptions
 * to standardized RFC-7807 compliant ErrorEnvelope schemas.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.warn("Validation error on request: {}", ex.getMessage());
        List<ErrorEnvelope.FieldErrorDetail> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(ErrorEnvelope.FieldErrorDetail.builder()
                    .field(fieldError.getField())
                    .issue(fieldError.getDefaultMessage())
                    .build());
        }

        ErrorEnvelope envelope = ErrorEnvelope.of("VALIDATION_FAILED", "Request payload validation check failed.", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(envelope);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorEnvelope> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        List<ErrorEnvelope.FieldErrorDetail> details = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> details.add(ErrorEnvelope.FieldErrorDetail.builder()
                .field(violation.getPropertyPath().toString())
                .issue(violation.getMessage())
                .build()));

        ErrorEnvelope envelope = ErrorEnvelope.of("CONSTRAINT_VIOLATION", "Query or parameter constraint violated.", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(envelope);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorEnvelope envelope = ErrorEnvelope.of("RESOURCE_NOT_FOUND", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(envelope);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorEnvelope> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Authentication failure: {}", ex.getMessage());
        ErrorEnvelope envelope = ErrorEnvelope.of("INVALID_CREDENTIALS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(envelope);
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    public ResponseEntity<ErrorEnvelope> handleUnauthorizedOperation(UnauthorizedOperationException ex) {
        log.warn("Authorization failure: {}", ex.getMessage());
        ErrorEnvelope envelope = ErrorEnvelope.of("FORBIDDEN", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(envelope);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorEnvelope> handleInsufficientStock(InsufficientStockException ex) {
        log.warn("Insufficient inventory stock: {}", ex.getMessage());
        ErrorEnvelope envelope = ErrorEnvelope.of("INSUFFICIENT_STOCK", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(envelope);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorEnvelope> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Duplicate resource conflict: {}", ex.getMessage());
        ErrorEnvelope envelope = ErrorEnvelope.of("DUPLICATE_RESOURCE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(envelope);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorEnvelope> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        ErrorEnvelope envelope = ErrorEnvelope.of(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(envelope);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> handleGeneralException(Exception ex) {
        log.error("Unhandled internal server error occurred: ", ex);
        ErrorEnvelope envelope = ErrorEnvelope.of("INTERNAL_SERVER_ERROR", "An unexpected server error occurred. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(envelope);
    }
}
