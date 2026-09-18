package com.bookcorner.security;

import com.bookcorner.common.exception.UnauthorizedOperationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Enterprise Security Utility helpers for querying the current Spring Security context,
 * identifying the active user, checking roles, and enforcing identity boundaries.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Prevent direct instantiation
    }

    /**
     * Retrieves the current active Authentication object from the SecurityContextHolder.
     */
    public static Optional<Authentication> getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }

    /**
     * Extracts the UserPrincipal from the SecurityContextHolder if present.
     */
    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        return getAuthentication()
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof UserPrincipal)
                .map(principal -> (UserPrincipal) principal);
    }

    /**
     * Extracts the UUID of the authenticated registered user (non-guest).
     */
    public static Optional<UUID> getCurrentUserId() {
        return getCurrentUserPrincipal()
                .filter(p -> !p.isGuest())
                .map(UserPrincipal::getId);
    }

    /**
     * Extracts the UUID of the authenticated registered user, throwing an exception if absent.
     */
    public static UUID getRequiredCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedOperationException("User must be authenticated to perform this operation."));
    }

    /**
     * Extracts the email of the authenticated registered user.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUserPrincipal()
                .filter(p -> !p.isGuest())
                .map(UserPrincipal::getEmail);
    }

    /**
     * Tests whether the authenticated user has the specified role authority.
     *
     * @param role role code, with or without 'ROLE_' prefix
     */
    public static boolean hasRole(String role) {
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getAuthentication()
                .map(Authentication::getAuthorities)
                .stream()
                .flatMap(Collection::stream)
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roleWithPrefix::equalsIgnoreCase);
    }

    /**
     * Indicates if the current security context represents an anonymous guest session.
     */
    public static boolean isGuest() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::isGuest)
                .orElse(false) || hasRole("ROLE_GUEST");
    }

    /**
     * Indicates if the authenticated caller has the CUSTOMER role.
     */
    public static boolean isCustomer() {
        return hasRole("ROLE_CUSTOMER");
    }

    /**
     * Indicates if the authenticated caller has the ADMIN role.
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Indicates whether an authenticated, non-guest user is currently bound to the execution thread.
     */
    public static boolean isAuthenticated() {
        return getCurrentUserId().isPresent();
    }
}
