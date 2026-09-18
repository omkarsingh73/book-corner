package com.bookcorner.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 6 / Spring Boot 3 Central Security Configuration.
 * Enforces stateless JWT bearer authentication, cross-origin resource sharing (CORS),
 * exception interception, and fine-grained Role-Based Access Control (RBAC) rules.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * BCrypt password encoder configured with standard cost factor 12.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Standard DAO authentication provider linking CustomUserDetailsService and BCryptPasswordEncoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Exposes the AuthenticationManager bean for programmatic authentication.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Primary HTTP Security filter chain definition.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF for stateless REST architecture
            .csrf(AbstractHttpConfigurer::disable)

            // 2. Enable CORS using registered configuration source
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 3. Set session management policy to STATELESS
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. Register custom authentication entry point and access denied handlers
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // 5. Wire DAO authentication provider
            .authenticationProvider(authenticationProvider())

            // 6. Prepend JWT bearer filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

            // 7. Enforce URL authorization rules based on RBAC roles (GUEST, CUSTOMER, ADMIN)
            .authorizeHttpRequests(auth -> auth
                // OpenAPI / Swagger UI / Webjars / Actuator Health (Public)
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info"
                ).permitAll()

                // Anonymous & Public Authentication APIs
                .requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/auth/guest-session",
                    "/auth/refresh"
                ).permitAll()

                // Catalog, Taxonomy, Authors, and Discovery APIs (Public Read-Only)
                .requestMatchers(HttpMethod.GET,
                    "/books",
                    "/books/**",
                    "/categories",
                    "/categories/**",
                    "/authors",
                    "/authors/**",
                    "/publishers",
                    "/publishers/**",
                    "/search/**",
                    "/recommendations/**"
                ).permitAll()

                // Book reviews viewing (Public Read-Only)
                .requestMatchers(HttpMethod.GET, "/reviews/books/**").permitAll()

                // Shipping rates calculation and consignment tracking (Public)
                .requestMatchers(HttpMethod.POST, "/shipping/rates").permitAll()
                .requestMatchers(HttpMethod.GET, "/shipping/consignments/**").permitAll()

                // External asynchronous payment provider webhooks (Public)
                .requestMatchers(HttpMethod.POST, "/payments/webhooks/**").permitAll()

                // Cart operations: accessible by Guests (with session token), Customers, and Admins
                .requestMatchers("/cart/**").hasAnyRole("GUEST", "CUSTOMER", "ADMIN")

                // Authenticated Customer operations (Customer and Admin)
                .requestMatchers(
                    "/auth/logout",
                    "/users/me/**",
                    "/wishlists/**",
                    "/orders/**",
                    "/payments/intent",
                    "/payments/wallet/**",
                    "/cart/merge"
                ).hasAnyRole("CUSTOMER", "ADMIN")

                // Customer reviews submission and voting
                .requestMatchers(HttpMethod.POST, "/books/*/reviews").hasAnyRole("CUSTOMER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/reviews/*/votes").hasAnyRole("CUSTOMER", "ADMIN")

                // Administrative management endpoints (Admin only)
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/books", "/books/**", "/categories/**", "/authors/**", "/publishers/**", "/coupons/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/books/**", "/categories/**", "/authors/**", "/publishers/**", "/coupons/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/books/**", "/categories/**", "/authors/**", "/publishers/**", "/coupons/**").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Deny unauthenticated access to any unspecified endpoints
                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Cross-Origin Resource Sharing (CORS) bean configuration allowing secure frontend SPA interactions.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Guest-Session-Token",
            "X-Store-Code",
            "Idempotency-Key",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Guest-Session-Token",
            "X-Total-Count",
            "Link"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
