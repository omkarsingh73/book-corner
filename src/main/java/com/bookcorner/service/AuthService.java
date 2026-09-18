package com.bookcorner.service;

import com.bookcorner.common.exception.DuplicateResourceException;
import com.bookcorner.common.exception.InvalidCredentialsException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.auth.AuthTokenResponse;
import com.bookcorner.dto.auth.LoginRequest;
import com.bookcorner.dto.auth.RegisterRequest;
import com.bookcorner.entity.member.GuestSessionEntity;
import com.bookcorner.entity.member.RoleEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.mapper.UserMapper;
import com.bookcorner.repository.member.GuestSessionRepository;
import com.bookcorner.repository.member.RoleRepository;
import com.bookcorner.repository.member.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

/**
 * Authentication and Session Management Service.
 * Coordinates customer registration, credential verification, and guest session initialization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GuestSessionRepository guestSessionRepository;
    private final UserMapper userMapper;
    private final CartService cartService;

    /**
     * Registers a new customer profile and issues tokens.
     */
    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        log.info("Attempting customer registration for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            log.warn("Registration rejected - duplicate email: {}", request.getEmail());
            throw new DuplicateResourceException("An account with email " + request.getEmail() + " already exists.");
        }

        RoleEntity customerRole = roleRepository.findByRoleCode("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .roleCode("ROLE_CUSTOMER")
                        .description("Standard authenticated customer")
                        .build()));

        UserEntity user = UserEntity.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(hashPassword(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phoneNumber(request.getPhoneNumber())
                .accountStatus("ACTIVE")
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build();

        UserEntity savedUser = userRepository.save(user);
        log.info("Customer registered successfully with ID: {}", savedUser.getId());

        // Merge guest cart if guest session token was provided
        if (request.getGuestSessionToken() != null && !request.getGuestSessionToken().isBlank()) {
            try {
                cartService.mergeGuestCart(savedUser.getId(), request.getGuestSessionToken());
            } catch (Exception e) {
                log.warn("Failed to merge guest cart during registration for user: {}", savedUser.getId(), e);
            }
        }

        return generateTokenResponse(savedUser);
    }

    /**
     * Authenticates customer credentials and issues fresh JWT access and refresh tokens.
     */
    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        log.info("Processing login request for email: {}", request.getEmail());

        UserEntity user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found for email: {}", request.getEmail());
                    return new InvalidCredentialsException("Invalid email or password.");
                });

        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            log.warn("Login rejected for user {}: account status is {}", user.getId(), user.getAccountStatus());
            throw new InvalidCredentialsException("Account is " + user.getAccountStatus() + ". Please contact customer support.");
        }

        if (!verifyPassword(request.getPassword(), user.getPasswordHash())) {
            userRepository.incrementFailedLoginAttempts(user.getId());
            log.warn("Invalid password attempt for user: {}", user.getId());
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        userRepository.recordSuccessfulLogin(user.getId(), Instant.now());
        log.info("Customer login authenticated successfully for user: {}", user.getId());

        // Merge guest basket into authenticated user cart if provided
        if (request.getGuestSessionToken() != null && !request.getGuestSessionToken().isBlank()) {
            try {
                cartService.mergeGuestCart(user.getId(), request.getGuestSessionToken());
            } catch (Exception e) {
                log.warn("Failed to merge guest cart during login for user: {}", user.getId(), e);
            }
        }

        return generateTokenResponse(user);
    }

    /**
     * Initializes an anonymous cryptographically signed guest session.
     */
    @Transactional
    public GuestSessionEntity startGuestSession(UUID storeId, String ipAddress, String userAgent) {
        String token = "gst_" + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(30, ChronoUnit.DAYS);

        GuestSessionEntity session = GuestSessionEntity.builder()
                .sessionToken(token)
                .storeId(storeId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(expiresAt)
                .build();

        GuestSessionEntity saved = guestSessionRepository.save(session);
        log.info("Initialized guest session token: {} (expires: {})", token, expiresAt);
        return saved;
    }

    private AuthTokenResponse generateTokenResponse(UserEntity user) {
        String mockJwtAccessToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9." +
                Base64.getUrlEncoder().withoutPadding().encodeToString(
                        ("{\"sub\":\"" + user.getId() + "\",\"email\":\"" + user.getEmail() + "\"}").getBytes(StandardCharsets.UTF_8)
                ) + ".mockSignature";

        String mockRefreshToken = "rft_" + UUID.randomUUID().toString().replace("-", "");

        return AuthTokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(mockJwtAccessToken)
                .expiresIn(900L) // 15 minutes
                .refreshToken(mockRefreshToken)
                .refreshExpiresIn(604800L) // 7 days
                .user(userMapper.toUserSummary(user))
                .build();
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cryptographic algorithm unavailable", e);
        }
    }

    private boolean verifyPassword(String rawPassword, String storedHash) {
        return hashPassword(rawPassword).equals(storedHash);
    }
}
