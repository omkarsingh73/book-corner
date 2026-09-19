package com.bookcorner.integration;

import com.bookcorner.security.JwtProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

/**
 * Base abstract test container harness for end-to-end full Spring Boot integration tests.
 * Manages a shared, high-performance PostgreSQL 16 container instance, provides WebMvc MockMvc harness,
 * and exposes JWT credential utilities.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookcorner_it")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-schemas.sql")
            .withReuse(true);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtProvider jwtProvider;

    @Autowired(required = false)
    protected CacheManager cacheManager;

    @BeforeEach
    void clearAllCaches() {
        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            }
        }
    }

    static {
        postgres.start();
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS member;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS catalog;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS ordering;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS payment;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS shipping;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS store;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS review;");
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to initialize test postgres database schemas", e);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "public");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    /**
     * Generates a signed JWT Bearer token with CUSTOMER role.
     */
    protected String generateCustomerToken(UUID userId, String email) {
        return jwtProvider.generateAccessToken(userId, email, List.of("CUSTOMER"));
    }

    /**
     * Generates a signed JWT Bearer token with ADMIN role.
     */
    protected String generateAdminToken(UUID userId, String email) {
        return jwtProvider.generateAccessToken(userId, email, List.of("ADMIN"));
    }

    /**
     * Creates an HttpHeaders object populated with Authorization Bearer header.
     */
    protected HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
