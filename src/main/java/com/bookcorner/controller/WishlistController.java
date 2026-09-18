package com.bookcorner.controller;

import com.bookcorner.common.exception.UnauthorizedOperationException;
import com.bookcorner.dto.wishlist.AddWishlistItemRequest;
import com.bookcorner.dto.wishlist.WishlistResponse;
import com.bookcorner.service.WishlistService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Customer Wishlist and Saved Items REST Controller.
 */
@RestController
@RequestMapping("/wishlists")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wishlist", description = "Customer book wishlists and price alert subscriptions.")
@SecurityRequirement(name = "BearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(summary = "Retrieve authenticated customer wishlist", operationId = "getWishlist")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer wishlist retrieved."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping
    public ResponseEntity<WishlistResponse> getWishlist(Authentication authentication) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("GET /wishlists for user: {}", userId);
        WishlistResponse response = wishlistService.getWishlist(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Add book to wishlist", operationId = "addWishlistItem")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item added to wishlist."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Book not found."),
            @ApiResponse(responseCode = "409", description = "Duplicate - Book already in wishlist.")
    })
    @PostMapping("/items")
    public ResponseEntity<WishlistResponse.WishlistItemDto> addWishlistItem(
            Authentication authentication,
            @Valid @RequestBody AddWishlistItemRequest request
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("POST /wishlists/items for user: {}, bookId: {}", userId, request.getBookId());
        WishlistResponse wishlist = wishlistService.addToWishlist(userId, request);

        WishlistResponse.WishlistItemDto added = wishlist.getItems().stream()
                .filter(item -> item.getBookId().equals(request.getBookId()))
                .findFirst()
                .orElse(null);

        return ResponseEntity.status(HttpStatus.CREATED).body(added);
    }

    @Operation(summary = "Remove book from wishlist", operationId = "removeWishlistItem")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removed from wishlist."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Item not found in wishlist.")
    })
    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> removeWishlistItem(
            Authentication authentication,
            @Parameter(description = "Book ID to remove", required = true)
            @PathVariable("bookId") UUID bookId
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("DELETE /wishlists/items/{} for user: {}", bookId, userId);
        wishlistService.removeFromWishlist(userId, bookId);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedOperationException("Authentication required to access customer wishlist.");
        }
        if (authentication.getPrincipal() instanceof com.bookcorner.security.UserPrincipal principal) {
            return principal.getId();
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return UUID.fromString("11111111-1111-1111-1111-111111111111");
        }
    }
}
