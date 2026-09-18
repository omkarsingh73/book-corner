package com.bookcorner.integration;

import com.bookcorner.entity.member.RoleEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.repository.member.RoleRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Security & RBAC Authorization Integration Tests (PostgreSQL Testcontainer)")
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtProvider jwtProvider;

    private UserEntity customerUser;
    private UserEntity adminUser;
    private String customerJwt;
    private String adminJwt;

    @BeforeEach
    void setUpUsersAndRoles() {
        RoleEntity customerRole = roleRepository.findByRoleCode("CUSTOMER")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .roleCode("CUSTOMER")
                        .description("Standard Customer")
                        .build()));

        RoleEntity adminRole = roleRepository.findByRoleCode("ADMIN")
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .roleCode("ADMIN")
                        .description("Administrator")
                        .build()));

        String customerEmail = "customer_sec_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        customerUser = UserEntity.builder()
                .email(customerEmail)
                .passwordHash("$2a$12$e8Y5t1pQ6hYF9V5u5z2zPeA...")
                .firstName("Customer")
                .lastName("Sec")
                .accountStatus("ACTIVE")
                .build();
        customerUser.getRoles().add(customerRole);
        customerUser = userRepository.save(customerUser);
        customerJwt = jwtProvider.generateAccessToken(customerUser.getId(), customerUser.getEmail(), List.of("CUSTOMER"));

        String adminEmail = "admin_sec_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        adminUser = UserEntity.builder()
                .email(adminEmail)
                .passwordHash("$2a$12$e8Y5t1pQ6hYF9V5u5z2zPeA...")
                .firstName("Admin")
                .lastName("Sec")
                .accountStatus("ACTIVE")
                .build();
        adminUser.getRoles().add(adminRole);
        adminUser = userRepository.save(adminUser);
        adminJwt = jwtProvider.generateAccessToken(adminUser.getId(), adminUser.getEmail(), List.of("ADMIN"));
    }

    @Test
    @DisplayName("Security 1: Anonymous requests to public endpoints must succeed (200 OK / 201 Created)")
    void shouldAllowAnonymousAccessToPublicEndpoints() throws Exception {
        // Public Catalog
        mockMvc.perform(get("/books")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Public Categories
        mockMvc.perform(get("/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Public Guest Session creation
        mockMvc.perform(post("/auth/guest-session")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Security 2: Anonymous requests to customer-protected endpoints must be rejected (401/403)")
    void shouldDenyAnonymousAccessToProtectedEndpoints() throws Exception {
        // Orders endpoint requires authentication
        mockMvc.perform(get("/orders")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

        // Wallet endpoint requires authentication
        mockMvc.perform(get("/payments/wallet")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

        // Cart merge requires authentication
        mockMvc.perform(post("/cart/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestSessionToken\":\"gst_anon\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Security 3: Authenticated Customer must be authorized to access customer endpoints")
    void shouldAllowCustomerToAccessCustomerEndpoints() throws Exception {
        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + customerJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/payments/wallet")
                        .header("Authorization", "Bearer " + customerJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security 4: Authenticated Customer attempting to access Admin endpoints must receive 403 Forbidden")
    void shouldDenyCustomerFromAccessingAdminEndpoints() throws Exception {
        // Customer attempting to create a book format (admin only)
        mockMvc.perform(post("/books")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Book\"}"))
                .andExpect(status().isForbidden());

        // Customer attempting to access admin actuator or administration
        mockMvc.perform(get("/actuator/beans")
                        .header("Authorization", "Bearer " + customerJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security 5: Anonymous Guest Session token grants access to /cart but denied for /orders")
    void shouldEnforceGuestSessionBoundaries() throws Exception {
        String guestToken = "gst_security_test_token";

        // Guest token allows access to cart
        mockMvc.perform(get("/cart")
                        .header("X-Guest-Session-Token", guestToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Guest token is forbidden from accessing customer orders
        mockMvc.perform(get("/orders")
                        .header("X-Guest-Session-Token", guestToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security 6: Malformed JWT token must be rejected with 4xx client error")
    void shouldRejectMalformedJwt() throws Exception {
        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer invalid.malformed.token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Security 7: Tampered cryptographic JWT signature must be rejected")
    void shouldRejectTamperedJwtSignature() throws Exception {
        // Alter the last character of the signature
        String tamperedJwt = customerJwt.substring(0, customerJwt.length() - 4) + "XXXX";

        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + tamperedJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Security 8: Expired JWT token must be rejected")
    void shouldRejectExpiredJwtToken() throws Exception {
        // Use reflection or private helper to generate an expired token
        Method method = JwtProvider.class.getDeclaredMethod("generateToken", UUID.class, String.class, List.class, long.class);
        method.setAccessible(true);
        String expiredToken = (String) method.invoke(jwtProvider, customerUser.getId(), customerUser.getEmail(), List.of("CUSTOMER"), -3600L);

        mockMvc.perform(get("/orders")
                        .header("Authorization", "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}
