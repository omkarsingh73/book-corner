package com.bookcorner.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when requested format SKU quantity exceeds available warehouse balance.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientStockException extends RuntimeException {

    private UUID formatId;
    private String sku;
    private final int requested;
    private final int available;

    public InsufficientStockException(UUID formatId, int requested, int available) {
        super(String.format("Insufficient inventory for format ID '%s': requested %d, available %d", formatId, requested, available));
        this.formatId = formatId;
        this.requested = requested;
        this.available = available;
    }

    public InsufficientStockException(String sku, int requested, int available) {
        super(String.format("Insufficient inventory for SKU '%s': requested %d, available %d", sku, requested, available));
        this.sku = sku;
        this.requested = requested;
        this.available = available;
    }

    public InsufficientStockException(String message) {
        super(message);
        this.requested = 0;
        this.available = 0;
    }

    public UUID getFormatId() {
        return formatId;
    }

    public String getSku() {
        return sku;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
