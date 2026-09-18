package com.bookcorner.controller;

import com.bookcorner.dto.auth.AuthTokenResponse;
import com.bookcorner.dto.auth.GuestSessionResponse;
import com.bookcorner.dto.auth.LoginRequest;
import com.bookcorner.dto.auth.RefreshTokenRequest;
import com.bookcorner.dto.auth.RegisterRequest;
import com.bookcorner.dto.auth.TokenRotationResponse;
import com.bookcorner.dto.common.GenericMessageResponse;
import com.bookcorner.entity.member.GuestSessionEntity;
import com.bookcorner.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Authentication and Session Management REST Controller.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Guest session management, credential authentication, JWT token issuance and rotation.")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Initialize an anonymous guest session", operationId = "startGuestSession")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Guest session created successfully."),
            @ApiResponse(responseCode = "429", description = "Too Many Requests."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @PostMapping("/guest-session")
    public ResponseEntity<GuestSessionResponse> startGuestSession(
            @Parameter(description = "Target store code tenant", example = "BK_MAIN_ONLINE")
            @RequestHeader(value = "X-Store-Code", required = false) String storeCode,
            HttpServletRequest request
    ) {
        log.info("Received request to start guest session for storeCode: {}", storeCode);
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        GuestSessionEntity session = authService.startGuestSession(null, ipAddress, userAgent);

        GuestSessionResponse response = GuestSessionResponse.builder()
                .guestSessionToken(session.getSessionToken())
                .expiresAt(session.getExpiresAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Register a new customer account", operationId = "registerCustomer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request / Validation Failure."),
            @ApiResponse(responseCode = "409", description = "Conflict - Duplicate email address."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        AuthTokenResponse tokenResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenResponse);
    }

    @Operation(summary = "Authenticate user credentials", operationId = "loginUser")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "401", description = "Invalid credentials."),
            @ApiResponse(responseCode = "403", description = "Account locked or suspended.")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request for email: {}", request.getEmail());
        AuthTokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "Rotate refresh token", operationId = "refreshToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token.")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRotationResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Received token rotation refresh request");
        TokenRotationResponse rotationResponse = authService.refreshToken(request);
        return ResponseEntity.ok(rotationResponse);
    }

    @Operation(summary = "Invalidate user session", operationId = "logoutUser",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping("/logout")
    public ResponseEntity<GenericMessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Received logout request");
        authService.logout(request);
        return ResponseEntity.ok(GenericMessageResponse.of("Logged out successfully. Tokens invalidated."));
    }
}
