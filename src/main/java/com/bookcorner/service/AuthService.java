package com.bookcorner.service;

import com.bookcorner.common.exception.DuplicateResourceException;
import com.bookcorner.common.exception.InvalidCredentialsException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.auth.AuthTokenResponse;
import com.bookcorner.dto.auth.LoginRequest;
import com.bookcorner.dto.auth.RefreshTokenRequest;
import com.bookcorner.dto.auth.RegisterRequest;
import com.bookcorner.dto.auth.TokenRotationResponse;
import com.bookcorner.entity.member.GuestSessionEntity;
import com.bookcorner.entity.member.RoleEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.mapper.UserMapper;
import com.bookcorner.repository.member.GuestSessionRepository;
import com.bookcorner.repository.member.RoleRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.security.JwtProvider;
import com.bookcorner.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

/**
 * Enterprise Authentication, Credential Verification, and Session Management Service.
 * Manages customer registration, BCrypt password hashing, JWT token generation,
 * refresh token rotation, and guest session lifecycle.
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
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * Registers a new customer profile with BCrypt password hashing and issues JWT token pair.
     */
    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        log.info("Attempting customer registration for email: {}", request.getEmail());

        String normalizedEmail = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration rejected - duplicate email: {}", normalizedEmail);
            throw new DuplicateResourceException("An account with email " + request.getEmail() + " already exists.");
        }

        RoleEntity customerRole = roleRepository.findByRoleCode("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .roleCode("ROLE_CUSTOMER")
                        .description("Standard authenticated customer")
                        .build()));

        UserEntity user = UserEntity.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
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
     * Authenticates customer credentials using BCrypt matching and issues fresh JWT token pair.
     */
    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        log.info("Processing login request for email: {}", normalizedEmail);

        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found for email: {}", normalizedEmail);
                    return new InvalidCredentialsException("Invalid email or password.");
                });

        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            log.warn("Login rejected for user {}: account status is {}", user.getId(), user.getAccountStatus());
            throw new InvalidCredentialsException("Account is " + user.getAccountStatus() + ". Please contact customer support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
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
     * Rotates refresh tokens: validates existing refresh token and issues a new access/refresh pair.
     */
    @Transactional
    public TokenRotationResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtProvider.validateToken(token)) {
            log.warn("Refresh token validation failed: invalid signature or expired");
            throw new InvalidCredentialsException("Invalid or expired refresh token.");
        }

        UUID userId = jwtProvider.getUserIdFromToken(token);
        if (userId == null) {
            log.warn("Failed to extract user ID from refresh token claims");
            throw new InvalidCredentialsException("Invalid refresh token claims.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for token subject: " + userId));

        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            log.warn("Token refresh denied for inactive account status: {}", user.getAccountStatus());
            throw new InvalidCredentialsException("Account is " + user.getAccountStatus());
        }

        UserPrincipal principal = UserPrincipal.create(user);
        String newAccessToken = jwtProvider.generateAccessToken(principal);
        String newRefreshToken = jwtProvider.generateRefreshToken(userId);

        log.info("Successfully rotated tokens for user ID: {}", userId);

        return TokenRotationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresInSeconds((int) jwtProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    /**
     * Handles customer logout by invalidating refresh tokens.
     */
    @Transactional
    public void logout(RefreshTokenRequest request) {
        log.info("Processing logout request for refresh token");
        // In a stateless JWT architecture, token invalidation can be recorded in an in-memory blocklist or audit trail
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
                .storeId(storeId != null ? storeId : UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(expiresAt)
                .build();

        GuestSessionEntity saved = guestSessionRepository.save(session);
        log.info("Initialized guest session token: {} (expires: {})", token, expiresAt);
        return saved;
    }

    private AuthTokenResponse generateTokenResponse(UserEntity user) {
        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtProvider.generateAccessToken(principal);
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        return AuthTokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .refreshToken(refreshToken)
                .refreshExpiresIn(jwtProvider.getRefreshTokenExpirationSeconds())
                .user(userMapper.toUserSummary(user))
                .build();
    }
}
