package com.bookcorner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when an order state transition is invalid or checkout validation fails.
 */
@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class OrderProcessingException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "ORDER_PROCESSING_FAILED";

    private final UUID orderId;
    private final String currentStatus;
    private final String attemptedStatus;

    public OrderProcessingException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.UNPROCESSABLE_ENTITY);
        this.orderId = null;
        this.currentStatus = null;
        this.attemptedStatus = null;
    }

    public OrderProcessingException(UUID orderId, String currentStatus, String attemptedStatus, String message) {
        super(String.format("Cannot transition order '%s' from status '%s' to '%s': %s",
                orderId, currentStatus, attemptedStatus, message), DEFAULT_ERROR_CODE, HttpStatus.UNPROCESSABLE_ENTITY);
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
    }
}
