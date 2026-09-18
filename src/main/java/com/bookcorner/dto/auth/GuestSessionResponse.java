package com.bookcorner.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * Guest session initialization response matching OpenAPI schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestSessionResponse implements Serializable {

    private String guestSessionToken;
    private Instant expiresAt;
}
