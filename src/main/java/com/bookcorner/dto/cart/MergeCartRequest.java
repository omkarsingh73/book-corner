package com.bookcorner.dto.cart;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Payload to merge guest basket into authenticated account.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartRequest implements Serializable {

    @NotBlank(message = "Guest session token is required")
    private String guestSessionToken;
}
