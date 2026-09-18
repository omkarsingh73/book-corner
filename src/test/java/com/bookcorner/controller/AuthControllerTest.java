package com.bookcorner.controller;

import com.bookcorner.common.exception.InvalidCredentialsException;
import com.bookcorner.dto.auth.AuthTokenResponse;
import com.bookcorner.dto.auth.LoginRequest;
import com.bookcorner.dto.auth.RefreshTokenRequest;
import com.bookcorner.dto.auth.RegisterRequest;
import com.bookcorner.dto.auth.TokenRotationResponse;
import com.bookcorner.entity.member.GuestSessionEntity;
import com.bookcorner.security.CustomAccessDeniedHandler;
import com.bookcorner.security.CustomAuthenticationEntryPoint;
import com.bookcorner.security.CustomUserDetailsService;
import com.bookcorner.security.JwtFilter;
import com.bookcorner.security.JwtProvider;
import com.bookcorner.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController WebMvc Tests (MockMvc & AssertJ)")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Test
    @DisplayName("POST /auth/guest-session should return 201 with guest session token")
    void shouldStartGuestSessionSuccessfully() throws Exception {
        GuestSessionEntity session = GuestSessionEntity.builder()
                .id(UUID.randomUUID())
                .sessionToken("gst_test_token_12345")
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        when(authService.startGuestSession(isNull(), any(), any())).thenReturn(session);

        mockMvc.perform(post("/auth/guest-session")
                        .header("X-Store-Code", "BK_MAIN_ONLINE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guestSessionToken").isEqualTo("gst_test_token_12345"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /auth/register should return 201 with access tokens for valid request")
    void shouldRegisterUserSuccessfully() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("Password123!")
                .firstName("Jane")
                .lastName("Doe")
                .build();

        AuthTokenResponse response = AuthTokenResponse.builder()
                .accessToken("jwt_access_token_abc")
                .refreshToken("jwt_refresh_token_xyz")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isEqualTo("jwt_access_token_abc"))
                .andExpect(jsonPath("$.refreshToken").isEqualTo("jwt_refresh_token_xyz"))
                .andExpect(jsonPath("$.tokenType").isEqualTo("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/register should return 400 Bad Request when email format is invalid")
    void shouldRejectRegistrationWithInvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email-format")
                .password("Password123!")
                .firstName("Jane")
                .lastName("Doe")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isEqualTo("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.invalidParams[0].name").isEqualTo("email"));
    }

    @Test
    @DisplayName("POST /auth/register should return 400 Bad Request when password does not meet complexity")
    void shouldRejectRegistrationWithWeakPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("valid@example.com")
                .password("weak") // Less than 8 characters, missing uppercase/digit/symbol
                .firstName("Jane")
                .lastName("Doe")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("POST /auth/login should return 200 with JWT tokens on valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("Password123!")
                .build();

        AuthTokenResponse response = AuthTokenResponse.builder()
                .accessToken("jwt_access_token_logged_in")
                .refreshToken("jwt_refresh_token_logged_in")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isEqualTo("jwt_access_token_logged_in"))
                .andExpect(jsonPath("$.refreshToken").isEqualTo("jwt_refresh_token_logged_in"));
    }

    @Test
    @DisplayName("POST /auth/login should return 401 Unauthorized when credentials are bad")
    void shouldReturn401OnInvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("WrongPassword123!")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password."));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").isEqualTo("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.detail").isEqualTo("Invalid email or password."));
    }

    @Test
    @DisplayName("POST /auth/refresh should rotate and return new tokens")
    void shouldRotateTokensSuccessfully() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid_refresh_token")
                .build();

        TokenRotationResponse response = TokenRotationResponse.builder()
                .accessToken("new_access_token")
                .refreshToken("new_refresh_token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isEqualTo("new_access_token"))
                .andExpect(jsonPath("$.refreshToken").isEqualTo("new_refresh_token"));
    }

    @Test
    @DisplayName("POST /auth/logout should invalidate token and return 200")
    void shouldLogoutSuccessfully() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("token_to_revoke")
                .build();

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully. Tokens invalidated."));

        verify(authService).logout(any(RefreshTokenRequest.class));
    }
}
