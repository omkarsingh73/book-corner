package com.bookcorner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when payment intent creation, capture, wallet debit, or payment gateway transactions fail.
 */
@Getter
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PaymentProcessingException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "PAYMENT_PROCESSING_FAILED";

    private final UUID transactionId;
    private final String paymentMethod;
    private final String gatewayErrorCode;

    public PaymentProcessingException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.UNPROCESSABLE_ENTITY);
        this.transactionId = null;
        this.paymentMethod = null;
        this.gatewayErrorCode = null;
    }

    public PaymentProcessingException(String message, String gatewayErrorCode) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.UNPROCESSABLE_ENTITY);
        this.transactionId = null;
        this.paymentMethod = null;
        this.gatewayErrorCode = gatewayErrorCode;
    }

    public PaymentProcessingException(String message, UUID transactionId, String paymentMethod, String gatewayErrorCode) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.UNPROCESSABLE_ENTITY);
        this.transactionId = transactionId;
        this.paymentMethod = paymentMethod;
        this.gatewayErrorCode = gatewayErrorCode;
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause, DEFAULT_ERROR_CODE, HttpStatus.UNPROCESSABLE_ENTITY);
        this.transactionId = null;
        this.paymentMethod = null;
        this.gatewayErrorCode = null;
    }
}
