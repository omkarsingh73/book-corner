package com.bookcorner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application Entry Point for Book Corner Platform.
 * Bootstraps the enterprise e-commerce backend service, initializes auto-configuration,
 * and starts component scanning across all domain packages.
 */
@SpringBootApplication
public class BookCornerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookCornerApplication.class, args);
    }
}
