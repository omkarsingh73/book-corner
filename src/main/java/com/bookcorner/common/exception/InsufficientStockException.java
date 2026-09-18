package com.bookcorner.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when requested format SKU inventory quantity exceeds available warehouse stock.
 */
@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientStockException extends BaseException {

    public static final String DEFAULT_ERROR_CODE = "INSUFFICIENT_STOCK";

    private final UUID formatId;
    private final String sku;
    private final int requested;
    private final int available;

    public InsufficientStockException(UUID formatId, int requested, int available) {
        super(String.format("Insufficient inventory for format ID '%s': requested %d, available %d",
                formatId, requested, available), DEFAULT_ERROR_CODE, HttpStatus.CONFLICT);
        this.formatId = formatId;
        this.sku = null;
        this.requested = requested;
        this.available = available;
    }

    public InsufficientStockException(String sku, int requested, int available) {
        super(String.format("Insufficient inventory for SKU '%s': requested %d, available %d",
                sku, requested, available), DEFAULT_ERROR_CODE, HttpStatus.CONFLICT);
        this.formatId = null;
        this.sku = sku;
        this.requested = requested;
        this.available = available;
    }

    public InsufficientStockException(String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.CONFLICT);
        this.formatId = null;
        this.sku = null;
        this.requested = 0;
        this.available = 0;
    }
}
