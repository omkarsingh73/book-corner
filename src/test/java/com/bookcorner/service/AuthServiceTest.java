package com.bookcorner.service;

import com.bookcorner.common.exception.DuplicateResourceException;
import com.bookcorner.common.exception.InvalidCredentialsException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests (Mockito & AssertJ)")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private GuestSessionRepository guestSessionRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CartService cartService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private UserEntity testUser;
    private RoleEntity customerRole;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        customerRole = RoleEntity.builder()
                .id(UUID.randomUUID())
                .roleCode("ROLE_CUSTOMER")
                .roleName("Customer")
                .build();

        testUser = UserEntity.builder()
                .id(userId)
                .email("clara@example.com")
                .passwordHash("$2a$12$encodedHashValue")
                .firstName("Clara")
                .lastName("Oswald")
                .accountStatus("ACTIVE")
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new customer and return JWT tokens")
    void shouldRegisterNewCustomerSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .email("clara@example.com")
                .password("StrongP@ss123")
                .firstName("Clara")
                .lastName("Oswald")
                .guestSessionToken("gst_guest123")
                .build();

        when(userRepository.existsByEmail("clara@example.com")).thenReturn(false);
        when(roleRepository.findByRoleCode("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("StrongP@ss123")).thenReturn("$2a$12$encodedHashValue");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        when(jwtProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock.access.token");
        when(jwtProvider.generateRefreshToken(testUser.getId())).thenReturn("mock.refresh.token");
        when(jwtProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
        when(userMapper.toUserSummary(testUser)).thenReturn(AuthTokenResponse.UserSummary.builder()
                .userId(testUser.getId())
                .email(testUser.getEmail())
                .roles(List.of("ROLE_CUSTOMER"))
                .build());

        AuthTokenResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("mock.refresh.token");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        assertThat(response.getUser().getEmail()).isEqualTo("clara@example.com");

        verify(cartService).mergeGuestCart(testUser.getId(), "gst_guest123");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should reject registration when email address already exists")
    void shouldRejectRegistrationForDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("clara@example.com")
                .password("StrongP@ss123")
                .firstName("Clara")
                .lastName("Oswald")
                .build();

        when(userRepository.existsByEmail("clara@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("Should successfully authenticate valid login credentials")
    void shouldLoginSuccessfully() {
        LoginRequest request = LoginRequest.builder()
                .email("clara@example.com")
                .password("StrongP@ss123")
                .build();

        when(userRepository.findByEmail("clara@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("StrongP@ss123", testUser.getPasswordHash())).thenReturn(true);

        when(jwtProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock.login.token");
        when(jwtProvider.generateRefreshToken(testUser.getId())).thenReturn("mock.login.refresh");
        when(jwtProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);
        when(jwtProvider.getRefreshTokenExpirationSeconds()).thenReturn(604800L);
        when(userMapper.toUserSummary(testUser)).thenReturn(AuthTokenResponse.UserSummary.builder()
                .userId(testUser.getId())
                .email(testUser.getEmail())
                .roles(List.of("ROLE_CUSTOMER"))
                .build());

        AuthTokenResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("mock.login.token");
        assertThat(response.getRefreshToken()).isEqualTo("mock.login.refresh");

        verify(userRepository).recordSuccessfulLogin(eq(testUser.getId()), any(Instant.class));
    }

    @Test
    @DisplayName("Should reject login when password does not match")
    void shouldRejectLoginForInvalidPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("clara@example.com")
                .password("WrongPassword")
                .build();

        when(userRepository.findByEmail("clara@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository).incrementFailedLoginAttempts(testUser.getId());
    }

    @Test
    @DisplayName("Should reject login when account status is not ACTIVE")
    void shouldRejectLoginForSuspendedAccount() {
        testUser.setAccountStatus("SUSPENDED");
        LoginRequest request = LoginRequest.builder()
                .email("clara@example.com")
                .password("StrongP@ss123")
                .build();

        when(userRepository.findByEmail("clara@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("SUSPENDED");
    }

    @Test
    @DisplayName("Should rotate refresh token when supplied with valid existing token")
    void shouldRotateRefreshTokenSuccessfully() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid.refresh.token")
                .build();

        when(jwtProvider.validateToken("valid.refresh.token")).thenReturn(true);
        when(jwtProvider.getUserIdFromToken("valid.refresh.token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(jwtProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("new.access.token");
        when(jwtProvider.generateRefreshToken(userId)).thenReturn("new.refresh.token");
        when(jwtProvider.getAccessTokenExpirationSeconds()).thenReturn(900L);

        TokenRotationResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("new.refresh.token");
        assertThat(response.getExpiresInSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("Should reject refresh when token is invalid or expired")
    void shouldRejectRefreshForInvalidToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid.or.expired.token")
                .build();

        when(jwtProvider.validateToken("invalid.or.expired.token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("Should initialize an anonymous guest session")
    void shouldStartGuestSession() {
        when(guestSessionRepository.save(any(GuestSessionEntity.class))).thenAnswer(invocation -> {
            GuestSessionEntity session = invocation.getArgument(0);
            session.setId(UUID.randomUUID());
            return session;
        });

        GuestSessionEntity session = authService.startGuestSession(null, "127.0.0.1", "JUnit-Agent");

        assertThat(session).isNotNull();
        assertThat(session.getSessionToken()).startsWith("gst_");
        assertThat(session.getExpiresAt()).isAfter(Instant.now());
        verify(guestSessionRepository).save(any(GuestSessionEntity.class));
    }
}
