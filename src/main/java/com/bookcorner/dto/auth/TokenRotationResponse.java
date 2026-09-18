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
    private String tokenType;

    public Long getExpiresIn() {
        return expiresInSeconds != null ? expiresInSeconds.longValue() : null;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresInSeconds = expiresIn != null ? expiresIn.intValue() : null;
    }

    public static class TokenRotationResponseBuilder {
        public TokenRotationResponseBuilder expiresIn(long expiresIn) {
            this.expiresInSeconds = (int) expiresIn;
            return this;
        }
    }
}
