package com.bookcorner.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Payload to cancel an order within policy grace period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderRequest implements Serializable {

    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;
}
