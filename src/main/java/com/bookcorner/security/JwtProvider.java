package com.bookcorner.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * High-performance, RFC-7519 compliant JSON Web Token (JWT) provider.
 * Implements HMAC-SHA256 asymmetric signature generation and timing-attack safe verification.
 */
@Component
@Slf4j
public class JwtProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Base64.Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.secret:bookcorner_jwt_super_secret_signing_key_2026_enterprise_grade_security_must_be_256_bits}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-seconds:900}") // 15 minutes
    private long accessTokenExpirationSeconds;

    @Value("${jwt.refresh-token-expiration-seconds:604800}") // 7 days
    private long refreshTokenExpirationSeconds;

    /**
     * Generates a signed JWT Access Token containing user claims and roles.
     */
    public String generateAccessToken(UserPrincipal principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        return generateToken(principal.getId(), principal.getEmail(), roles, accessTokenExpirationSeconds);
    }

    /**
     * Generates a signed JWT Access Token with custom claims.
     */
    public String generateAccessToken(UUID userId, String email, List<String> roles) {
        return generateToken(userId, email, roles, accessTokenExpirationSeconds);
    }

    /**
     * Generates an opaque or signed Refresh Token.
     */
    public String generateRefreshToken(UUID userId) {
        return generateToken(userId, null, Collections.emptyList(), refreshTokenExpirationSeconds);
    }

    /**
     * Validates cryptographic signature and expiration of a JWT token.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Invalid JWT structure: token does not contain 3 segments");
                return false;
            }

            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content, jwtSecret);

            if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("JWT signature verification failed");
                return false;
            }

            JsonNode payload = parsePayload(parts[1]);
            long exp = payload.path("exp").asLong(0);
            if (exp > 0 && exp < Instant.now().getEpochSecond()) {
                log.warn("JWT token has expired at timestamp: {}", exp);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts subject User ID from token claims.
     */
    public UUID getUserIdFromToken(String token) {
        try {
            JsonNode payload = extractPayload(token);
            String sub = payload.path("sub").asText();
            return UUID.fromString(sub);
        } catch (Exception e) {
            log.error("Failed to extract userId from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts email from token claims.
     */
    public String getEmailFromToken(String token) {
        try {
            JsonNode payload = extractPayload(token);
            return payload.path("email").asText(null);
        } catch (Exception e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts security roles list from token claims.
     */
    public List<String> getRolesFromToken(String token) {
        try {
            JsonNode payload = extractPayload(token);
            JsonNode rolesNode = payload.path("roles");
            List<String> roles = new ArrayList<>();
            if (rolesNode.isArray()) {
                for (JsonNode r : rolesNode) {
                    roles.add(r.asText());
                }
            }
            return roles;
        } catch (Exception e) {
            log.error("Failed to extract roles from token: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Extracts token expiration instant.
     */
    public Instant getExpirationFromToken(String token) {
        try {
            JsonNode payload = extractPayload(token);
            long exp = payload.path("exp").asLong(0);
            return Instant.ofEpochSecond(exp);
        } catch (Exception e) {
            return null;
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    // --- Internal Helpers ---

    private String generateToken(UUID userId, String email, List<String> roles, long expirationSeconds) {
        try {
            Instant now = Instant.now();
            Instant expiry = now.plusSeconds(expirationSeconds);

            Map<String, Object> headerMap = Map.of(
                    "alg", "HS256",
                    "typ", "JWT"
            );

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("sub", userId.toString());
            if (email != null) {
                payloadMap.put("email", email);
            }
            if (roles != null && !roles.isEmpty()) {
                payloadMap.put("roles", roles);
            }
            payloadMap.put("iat", now.getEpochSecond());
            payloadMap.put("exp", expiry.getEpochSecond());
            payloadMap.put("jti", UUID.randomUUID().toString());

            String headerEncoded = B64_ENCODER.encodeToString(objectMapper.writeValueAsBytes(headerMap));
            String payloadEncoded = B64_ENCODER.encodeToString(objectMapper.writeValueAsBytes(payloadMap));

            String content = headerEncoded + "." + payloadEncoded;
            String signature = sign(content, jwtSecret);

            return content + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    private JsonNode extractPayload(String token) throws Exception {
        String[] parts = token.split("\\.");
        return parsePayload(parts[1]);
    }

    private JsonNode parsePayload(String base64Payload) throws Exception {
        byte[] bytes = B64_DECODER.decode(base64Payload);
        return objectMapper.readTree(bytes);
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return B64_ENCODER.encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Cryptographic HMAC-SHA256 unavailable", e);
        }
    }
}
