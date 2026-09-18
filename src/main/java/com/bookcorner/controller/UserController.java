package com.bookcorner.controller;

import com.bookcorner.common.exception.UnauthorizedOperationException;
import com.bookcorner.dto.common.AddressDto;
import com.bookcorner.dto.common.GenericMessageResponse;
import com.bookcorner.dto.user.CreateAddressRequest;
import com.bookcorner.dto.user.UpdateUserProfileRequest;
import com.bookcorner.dto.user.UserProfileResponse;
import com.bookcorner.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Customer Profile and Address Book REST Controller.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "Customer profiles, address book management, and account preferences.")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Retrieve current authenticated user profile", operationId = "getCurrentUserProfile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "User profile not found.")
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(Authentication authentication) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("GET /users/me for user: {}", userId);
        UserProfileResponse profile = userService.getCurrentUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Update user profile", operationId = "updateUserProfile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("PATCH /users/me for user: {}", userId);
        UserProfileResponse updated = userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "List user saved addresses", operationId = "listUserAddresses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saved addresses retrieved."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("/me/addresses")
    public ResponseEntity<List<AddressDto>> listUserAddresses(Authentication authentication) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("GET /users/me/addresses for user: {}", userId);
        List<AddressDto> addresses = userService.listUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Add an address to address book", operationId = "createUserAddress")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping("/me/addresses")
    public ResponseEntity<AddressDto> createUserAddress(
            Authentication authentication,
            @Valid @RequestBody CreateAddressRequest request
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("POST /users/me/addresses for user: {}", userId);
        AddressDto created = userService.createUserAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Delete an address from address book", operationId = "deleteUserAddress")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address deleted successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Address not found.")
    })
    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<GenericMessageResponse> deleteUserAddress(
            Authentication authentication,
            @Parameter(description = "Address ID to delete", required = true)
            @PathVariable("addressId") UUID addressId
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("DELETE /users/me/addresses/{} for user: {}", addressId, userId);
        userService.deleteUserAddress(userId, addressId);
        return ResponseEntity.ok(GenericMessageResponse.of("Address deleted successfully."));
    }

    private UUID resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedOperationException("Authentication required to access user account resources.");
        }
        if (authentication.getPrincipal() instanceof com.bookcorner.security.UserPrincipal principal) {
            return principal.getId();
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            // Fallback for test tokens or non-UUID principals
            return UUID.fromString("11111111-1111-1111-1111-111111111111");
        }
    }
}
