package com.bookcorner.config;

import com.bookcorner.common.persistence.JpaAuditingConfig;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base abstract test container harness for Spring Data JPA repository slice tests.
 * Manages a shared, high-performance PostgreSQL 16 container instance.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@Import(JpaAuditingConfig.class)
public abstract class AbstractPostgresRepositoryTest {

    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("bookcorner_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-schemas.sql")
            .withReuse(true);

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
    }
}
