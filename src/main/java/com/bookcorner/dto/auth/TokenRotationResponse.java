package com.bookcorner.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token rotation refresh response matching OpenAPI schema.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRotationResponse implements Serializable {

    private String accessToken;
    private String refreshToken;
    private Integer expiresInSeconds;
}
