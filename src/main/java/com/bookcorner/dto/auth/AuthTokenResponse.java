package com.bookcorner.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * JWT token pair response issued upon successful login or registration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenResponse implements Serializable {

    @Builder.Default
    private String tokenType = "Bearer";

    private String accessToken;

    private Long expiresIn; // Seconds until access token expires

    private String refreshToken;

    private Long refreshExpiresIn; // Seconds until refresh token expires

    private UserSummary user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary implements Serializable {
        private UUID userId;
        private String email;
        private String firstName;
        private String lastName;
        private List<String> roles;
    }
}
