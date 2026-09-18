package com.bookcorner.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter intercepting incoming HTTP requests.
 * Extracts and verifies Bearer JWT tokens, sets SecurityContextHolder principals,
 * and attaches anonymous GUEST authorities when X-Guest-Session-Token header is provided.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);

            if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
                UUID userId = jwtProvider.getUserIdFromToken(jwt);
                String email = jwtProvider.getEmailFromToken(jwt);
                List<String> roles = jwtProvider.getRolesFromToken(jwt);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UserPrincipal principal = UserPrincipal.builder()
                        .id(userId)
                        .email(email != null ? email : userId.toString())
                        .password("")
                        .authorities(authorities)
                        .active(true)
                        .isGuest(false)
                        .build();

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user {} via JWT token with authorities: {}", userId, authorities);
            } else {
                // If no JWT Bearer token, check for anonymous Guest Session header
                String guestToken = request.getHeader("X-Guest-Session-Token");
                if (StringUtils.hasText(guestToken)) {
                    UserPrincipal guestPrincipal = UserPrincipal.createGuest(guestToken.trim());
                    UsernamePasswordAuthenticationToken guestAuth = new UsernamePasswordAuthenticationToken(
                            guestPrincipal,
                            null,
                            guestPrincipal.getAuthorities()
                    );
                    guestAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(guestAuth);
                    log.debug("Authenticated anonymous guest session with token: [PROTECTED]");
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication in security context: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
