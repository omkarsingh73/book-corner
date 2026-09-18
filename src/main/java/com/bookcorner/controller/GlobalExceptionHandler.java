package com.bookcorner.controller;

import com.bookcorner.common.exception.AccountStatusException;
import com.bookcorner.common.exception.BaseException;
import com.bookcorner.common.exception.BusinessRuleViolationException;
import com.bookcorner.common.exception.DuplicateResourceException;
import com.bookcorner.common.exception.InsufficientStockException;
import com.bookcorner.common.exception.InvalidCouponException;
import com.bookcorner.common.exception.InvalidCredentialsException;
import com.bookcorner.common.exception.InvalidTokenException;
import com.bookcorner.common.exception.OptimisticLockingException;
import com.bookcorner.common.exception.OrderProcessingException;
import com.bookcorner.common.exception.PaymentProcessingException;
import com.bookcorner.common.exception.ProblemDetails;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.common.exception.TokenExpiredException;
import com.bookcorner.common.exception.UnauthorizedOperationException;
import com.bookcorner.common.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Enterprise Global REST Exception Handler.
 * Intercepts, maps, and normalizes all application exceptions into RFC-7807/RFC-9457
 * Problem Details schemas while maintaining backward compatibility with the OpenAPI ErrorEnvelope schema.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String URN_PROBLEM_PREFIX = "urn:bookcorner:problem:";

    // =========================================================================
    // 1. Validation & Input Failure Handlers (400 Bad Request)
    // =========================================================================

    /**
     * Handles DTO Bean Validation failures (@Valid on request bodies).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetails> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.warn("Payload validation failed on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        List<ProblemDetails.InvalidParam> invalidParams = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            invalidParams.add(ProblemDetails.InvalidParam.of(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue()
            ));
        }

        ProblemDetails problem = buildProblemDetails(
                "validation-failed",
                "Validation Failed",
                HttpStatus.BAD_REQUEST,
                "Request body validation check failed. Please correct the invalid fields.",
                "VALIDATION_FAILED",
                request,
                invalidParams
        );

        return buildResponse(HttpStatus.BAD_REQUEST, problem);
    }

    /**
     * Handles Query Parameter / Path Variable constraint violations (@Validated).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetails> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Constraint violation on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        List<ProblemDetails.InvalidParam> invalidParams = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "unknown";
            invalidParams.add(ProblemDetails.InvalidParam.of(
                    propertyPath,
                    violation.getMessage(),
                    violation.getInvalidValue()
            ));
        });

        ProblemDetails problem = buildProblemDetails(
                "constraint-violation",
                "Constraint Violation",
                HttpStatus.BAD_REQUEST,
                "One or more query parameters or path variables violated constraint rules.",
                "CONSTRAINT_VIOLATION",
                request,
                invalidParams
        );

        return buildResponse(HttpStatus.BAD_REQUEST, problem);
    }

    /**
     * Handles custom domain-level programmatic validation failures.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetails> handleValidationException(
            ValidationException ex,
            HttpServletRequest request
    ) {
        log.warn("Domain validation failure on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "domain-validation-failed",
                "Validation Error",
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                ex.getErrorCode(),
                request,
                ex.getValidationErrors()
        );

        return buildResponse(HttpStatus.BAD_REQUEST, problem);
    }

    /**
     * Handles malformed, unparseable JSON payloads or incompatible type deserializations.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetails> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Malformed HTTP message body on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        String detailedMessage = "Request body is unparseable, malformed, or missing required JSON structure.";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            detailedMessage += " Root cause: " + ex.getCause().getMessage();
        }

        ProblemDetails problem = buildProblemDetails(
                "malformed-payload",
                "Malformed Request Payload",
                HttpStatus.BAD_REQUEST,
                detailedMessage,
                "MALFORMED_REQUEST_PAYLOAD",
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.BAD_REQUEST, problem);
    }

    /**
     * Handles type conversion failures (e.g. passing a string for a UUID or integer path variable).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetails> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Parameter '%s' expected value of type '%s' but received: '%s'",
                ex.getName(), requiredType, ex.getValue());
        log.warn("Argument type mismatch on endpoint [{}]: {}", request.getRequestURI(), message);

        ProblemDetails.InvalidParam param = ProblemDetails.InvalidParam.of(ex.getName(), "Expected type: " + requiredType, ex.getValue());

        ProblemDetails problem = buildProblemDetails(
                "type-mismatch",
                "Argument Type Mismatch",
                HttpStatus.BAD_REQUEST,
                message,
                "TYPE_MISMATCH",
                request,
                List.of(param)
        );

        return buildResponse(HttpStatus.BAD_REQUEST, problem);
    }

    /**
     * Handles missing required servlet query parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetails> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String message = String.format("Required query parameter '%s' of type '%s' is missing.",
                ex.getParameterName(), ex.getParameterType());
        log.warn("Missing parameter on endpoint [{}]: {}", request.getRequestURI(), message);

        ProblemDetails.InvalidParam param = ProblemDetails.InvalidParam.of(ex.getParameterName(), "Parameter is required");

        ProblemDetails problem = buildProblemDetails(
                "missing-parameter",
                "Missing Required Parameter",
                HttpStatus.BAD_REQUEST,
                message,
                "MISSING_PARAMETER",
                request,
                List.of(param)
        );

        return buildResponse(HttpStatus.BAD_REQUEST, problem);
    }

    /**
     * Handles missing required HTTP request headers.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetails> handleMissingRequestHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request
    ) {
        String message = String.format("Required request header '%s' is missing.", ex.getHeaderName());
        log.warn("Missing header on endpoint [{}]: {}", request.getRequestURI(), message);

        ProblemDetails problem = buildProblemDetails(
                "missing-header",
                "Missing Required Header",
                HttpStatus.BAD_REQUEST,
                message,
                "MISSING_HEADER",
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.BAD_REQUEST, problem);
    }

    // =========================================================================
    // 2. Business & Domain Exception Handlers
    // =========================================================================

    /**
     * Handles ResourceNotFoundException (404 Not Found).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetails> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "resource-not-found",
                "Resource Not Found",
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                ex.getErrorCode(),
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.NOT_FOUND, problem);
    }

    /**
     * Handles unmapped endpoints or static resources in Spring 6 (404 Not Found).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetails> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Route not mapped on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "endpoint-not-found",
                "Endpoint Not Found",
                HttpStatus.NOT_FOUND,
                "The requested resource or endpoint path was not found: " + request.getRequestURI(),
                "ENDPOINT_NOT_FOUND",
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.NOT_FOUND, problem);
    }

    /**
     * Handles DuplicateResourceException (409 Conflict).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetails> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {
        log.warn("Duplicate resource on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "duplicate-resource",
                "Duplicate Resource Conflict",
                HttpStatus.CONFLICT,
                ex.getMessage(),
                ex.getErrorCode(),
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.CONFLICT, problem);
    }

    /**
     * Handles InsufficientStockException (409 Conflict).
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ProblemDetails> handleInsufficientStock(
            InsufficientStockException ex,
            HttpServletRequest request
    ) {
        log.warn("Insufficient inventory on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "insufficient-stock",
                "Insufficient Inventory",
                HttpStatus.CONFLICT,
                ex.getMessage(),
                ex.getErrorCode(),
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.CONFLICT, problem);
    }

    /**
     * Handles OptimisticLockingException and Spring OptimisticLockingFailureException (409 Conflict).
     */
    @ExceptionHandler({OptimisticLockingException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ProblemDetails> handleOptimisticLockConflict(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Optimistic locking concurrency conflict on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "concurrency-conflict",
                "Concurrent Modification Conflict",
                HttpStatus.CONFLICT,
                "The resource has been updated or modified by another concurrent transaction. Please reload and retry.",
                "CONCURRENT_MODIFICATION_CONFLICT",
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.CONFLICT, problem);
    }

    /**
     * Handles relational database unique or foreign key constraint collisions (409 Conflict).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetails> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.error("Database integrity violation on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "data-integrity-violation",
                "Data Integrity Violation",
                HttpStatus.CONFLICT,
                "Database constraint violation occurred. A duplicate key or referenced record conflict was detected.",
                "DATA_INTEGRITY_VIOLATION",
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.CONFLICT, problem);
    }

    /**
     * Handles InvalidCouponException (422 Unprocessable Entity).
     */
    @ExceptionHandler(InvalidCouponException.class)
    public ResponseEntity<ProblemDetails> handleInvalidCoupon(
            InvalidCouponException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid coupon on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "invalid-coupon",
                "Invalid Promotional Coupon",
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                ex.getErrorCode(),
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    /**
     * Handles PaymentProcessingException (422 Unprocessable Entity).
     */
    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ProblemDetails> handlePaymentProcessing(
            PaymentProcessingException ex,
            HttpServletRequest request
    ) {
        log.error("Payment processing error on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "payment-failure",
                "Payment Processing Failed",
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                ex.getErrorCode(),
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    /**
     * Handles OrderProcessingException (422 Unprocessable Entity).
     */
    @ExceptionHandler(OrderProcessingException.class)
    public ResponseEntity<ProblemDetails> handleOrderProcessing(
            OrderProcessingException ex,
            HttpServletRequest request
    ) {
        log.warn("Order processing failure on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "order-processing-failed",
                "Order Processing Error",
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                ex.getErrorCode(),
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    /**
     * Handles BusinessRuleViolationException (422 Unprocessable Entity).
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ProblemDetails> handleBusinessRuleViolation(
            BusinessRuleViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Business rule violation on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "business-rule-violation",
                "Business Rule Violation",
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                ex.getErrorCode(),
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
    }

    /**
     * Catch-all for any application BaseException subclass not explicitly handled above.
     */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ProblemDetails> handleBaseException(
            BaseException ex,
            HttpServletRequest request
    ) {
        log.warn("Base domain exception on endpoint [{}]: status={} code={}: {}",
                request.getRequestURI(), ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                ex.getErrorCode().toLowerCase().replace("_", "-"),
                "Domain Processing Error",
                ex.getHttpStatus(),
                ex.getMessage(),
                ex.getErrorCode(),
                request,
                Collections.emptyList()
        );

        return buildResponse(ex.getHttpStatus(), problem);
    }

    // =========================================================================
    // 3. Security Exception Handlers (401 Unauthorized, 403 Forbidden)
    // =========================================================================

    /**
     * Handles InvalidCredentialsException, TokenExpiredException, InvalidTokenException,
     * and Spring Security AuthenticationException (401 Unauthorized).
     */
    @ExceptionHandler({
            InvalidCredentialsException.class,
            TokenExpiredException.class,
            InvalidTokenException.class,
            BadCredentialsException.class,
            AuthenticationException.class
    })
    public ResponseEntity<ProblemDetails> handleAuthenticationException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication failure on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        String code = "UNAUTHORIZED";
        if (ex instanceof BaseException be) {
            code = be.getErrorCode();
        } else if (ex instanceof BadCredentialsException) {
            code = "INVALID_CREDENTIALS";
        }

        ProblemDetails problem = buildProblemDetails(
                "unauthorized",
                "Authentication Required",
                HttpStatus.UNAUTHORIZED,
                ex.getMessage() != null ? ex.getMessage() : "Authentication credentials are missing or invalid.",
                code,
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.UNAUTHORIZED, problem);
    }

    /**
     * Handles UnauthorizedOperationException and Spring Security AccessDeniedException (403 Forbidden).
     */
    @ExceptionHandler({UnauthorizedOperationException.class, AccessDeniedException.class})
    public ResponseEntity<ProblemDetails> handleAccessDenied(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "forbidden",
                "Access Denied",
                HttpStatus.FORBIDDEN,
                "You do not possess sufficient authorization permissions to access this resource.",
                "FORBIDDEN",
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.FORBIDDEN, problem);
    }

    /**
     * Handles AccountStatusException, DisabledException, and LockedException (403 Forbidden).
     */
    @ExceptionHandler({AccountStatusException.class, DisabledException.class, LockedException.class})
    public ResponseEntity<ProblemDetails> handleAccountStatusException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.warn("Inactive or locked account access attempt on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "account-inactive",
                "Account Inactive or Locked",
                HttpStatus.FORBIDDEN,
                ex.getMessage(),
                "ACCOUNT_INACTIVE",
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.FORBIDDEN, problem);
    }

    // =========================================================================
    // 4. HTTP Protocol & Routing Handlers (405, 415, 406)
    // =========================================================================

    /**
     * Handles HttpRequestMethodNotSupportedException (405 Method Not Allowed).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetails> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        String message = String.format("HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        log.warn("HTTP method not allowed on endpoint [{}]: {}", request.getRequestURI(), message);

        ProblemDetails problem = buildProblemDetails(
                "method-not-allowed",
                "Method Not Allowed",
                HttpStatus.METHOD_NOT_ALLOWED,
                message,
                "METHOD_NOT_ALLOWED",
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, problem);
    }

    /**
     * Handles HttpMediaTypeNotSupportedException (415 Unsupported Media Type).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetails> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        String message = String.format("Media type '%s' is not supported. Supported media types: %s",
                ex.getContentType(), ex.getSupportedMediaTypes());
        log.warn("Unsupported media type on endpoint [{}]: {}", request.getRequestURI(), message);

        ProblemDetails problem = buildProblemDetails(
                "unsupported-media-type",
                "Unsupported Media Type",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                message,
                "UNSUPPORTED_MEDIA_TYPE",
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, problem);
    }

    /**
     * Handles HttpMediaTypeNotAcceptableException (406 Not Acceptable).
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetails> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex,
            HttpServletRequest request
    ) {
        log.warn("Media type not acceptable on endpoint [{}]: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetails problem = buildProblemDetails(
                "not-acceptable",
                "Not Acceptable",
                HttpStatus.NOT_ACCEPTABLE,
                "Cannot produce a response matching the Accept header requested by the client.",
                "NOT_ACCEPTABLE",
                request,
                Collections.emptyList()
        );

        return buildResponse(HttpStatus.NOT_ACCEPTABLE, problem);
    }

    // =========================================================================
    // 5. Unhandled Internal Server Error (500 Internal Server Error)
    // =========================================================================

    /**
     * Fallback handler for all unexpected or unhandled exceptions.
     * Sanitizes internal error details to prevent accidental credential or schema leaks.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetails> handleGeneralException(
            Exception ex,
            HttpServletRequest request
    ) {
        String requestId = extractRequestId(request);
        log.error("Unhandled internal server error on endpoint [{}] [RequestId: {}]: ",
                request.getRequestURI(), requestId, ex);

        ProblemDetails problem = ProblemDetails.builder()
                .type(URI.create(URN_PROBLEM_PREFIX + "internal-server-error"))
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected server error occurred while processing your request. Please reference this Request ID when contacting support.")
                .instance(URI.create(request.getRequestURI()))
                .code("INTERNAL_SERVER_ERROR")
                .requestId(requestId)
                .timestamp(Instant.now())
                .invalidParams(Collections.emptyList())
                .build();

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, problem);
    }

    // =========================================================================
    // Internal Helper Methods
    // =========================================================================

    private ProblemDetails buildProblemDetails(
            String typeSuffix,
            String title,
            HttpStatus status,
            String detail,
            String code,
            HttpServletRequest request,
            List<ProblemDetails.InvalidParam> invalidParams
    ) {
        return ProblemDetails.builder()
                .type(URI.create(URN_PROBLEM_PREFIX + typeSuffix))
                .title(title)
                .status(status.value())
                .detail(detail)
                .instance(URI.create(request.getRequestURI()))
                .code(code)
                .requestId(extractRequestId(request))
                .timestamp(Instant.now())
                .invalidParams(invalidParams != null ? invalidParams : Collections.emptyList())
                .build();
    }

    private ResponseEntity<ProblemDetails> buildResponse(HttpStatus status, ProblemDetails problem) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(problem, headers, status);
    }

    private String extractRequestId(HttpServletRequest request) {
        String header = request.getHeader("X-Request-Id");
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        header = request.getHeader("X-Correlation-Id");
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        return UUID.randomUUID().toString();
    }
}
