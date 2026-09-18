package com.bookcorner.controller;

import com.bookcorner.dto.cart.AddCartItemRequest;
import com.bookcorner.dto.cart.ApplyCouponRequest;
import com.bookcorner.dto.cart.CartResponse;
import com.bookcorner.dto.cart.MergeCartRequest;
import com.bookcorner.dto.cart.UpdateCartItemQuantityRequest;
import com.bookcorner.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Shopping Cart and Basket Lifecycle REST Controller.
 */
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Shopping basket lifecycle, guest cart carryover, item quantity mutations, and discounts.")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Retrieve active shopping cart", operationId = "getActiveCart",
            security = {@SecurityRequirement(name = "BearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active cart details."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "404", description = "Cart not found.")
    })
    @GetMapping
    public ResponseEntity<CartResponse> getActiveCart(
            @Parameter(description = "Guest session token")
            @RequestHeader(value = "X-Guest-Session-Token", required = false) String guestSessionToken,
            Authentication authentication
    ) {
        UUID userId = resolveOptionalUserId(authentication);
        log.info("GET /cart for userId: {}, guestToken present: {}", userId, guestSessionToken != null);
        CartResponse cart = cartService.getCart(userId, guestSessionToken);
        return ResponseEntity.ok(cart);
    }

    @Operation(summary = "Add format SKU item to cart", operationId = "addItemToCart",
            security = {@SecurityRequirement(name = "BearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "404", description = "Format SKU not found."),
            @ApiResponse(responseCode = "409", description = "Insufficient inventory stock."),
            @ApiResponse(responseCode = "422", description = "Unprocessable Entity.")
    })
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(
            @Parameter(description = "Guest session token")
            @RequestHeader(value = "X-Guest-Session-Token", required = false) String guestSessionToken,
            Authentication authentication,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        UUID userId = resolveOptionalUserId(authentication);
        log.info("POST /cart/items: formatId={}, qty={}", request.getFormatId(), request.getQuantity());
        CartResponse updated = cartService.addItemToCart(userId, guestSessionToken, null, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Mutate cart item quantity", operationId = "updateCartItemQuantity",
            security = {@SecurityRequirement(name = "BearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cart updated successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "404", description = "Format item not in cart."),
            @ApiResponse(responseCode = "409", description = "Insufficient stock.")
    })
    @PatchMapping("/items/{formatId}")
    public ResponseEntity<CartResponse> updateCartItemQuantity(
            @Parameter(description = "Format SKU ID", required = true)
            @PathVariable("formatId") UUID formatId,
            @Parameter(description = "Guest session token")
            @RequestHeader(value = "X-Guest-Session-Token", required = false) String guestSessionToken,
            Authentication authentication,
            @Valid @RequestBody UpdateCartItemQuantityRequest request
    ) {
        UUID userId = resolveOptionalUserId(authentication);
        log.info("PATCH /cart/items/{}: newQty={}", formatId, request.getQuantity());
        CartResponse updated = cartService.updateItemQuantity(userId, guestSessionToken, formatId, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Remove item from cart", operationId = "removeCartItem",
            security = {@SecurityRequirement(name = "BearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item deleted successfully."),
            @ApiResponse(responseCode = "404", description = "Item not found in cart.")
    })
    @DeleteMapping("/items/{formatId}")
    public ResponseEntity<Void> removeCartItem(
            @Parameter(description = "Format SKU ID", required = true)
            @PathVariable("formatId") UUID formatId,
            @Parameter(description = "Guest session token")
            @RequestHeader(value = "X-Guest-Session-Token", required = false) String guestSessionToken,
            Authentication authentication
    ) {
        UUID userId = resolveOptionalUserId(authentication);
        log.info("DELETE /cart/items/{}", formatId);
        cartService.removeItemFromCart(userId, guestSessionToken, formatId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Synchronize guest cart into authenticated user cart", operationId = "mergeGuestCart",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carts merged successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping("/merge")
    public ResponseEntity<CartResponse> mergeGuestCart(
            Authentication authentication,
            @Valid @RequestBody MergeCartRequest request
    ) {
        UUID userId = resolveRequiredUserId(authentication);
        log.info("POST /cart/merge for user: {}", userId);
        CartResponse merged = cartService.mergeGuestCart(userId, request.getGuestSessionToken());
        return ResponseEntity.ok(merged);
    }

    @Operation(summary = "Apply promotional coupon code to active cart", operationId = "applyCartCoupon",
            security = {@SecurityRequirement(name = "BearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon applied successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request / Coupon Invalid.")
    })
    @PostMapping("/coupon")
    public ResponseEntity<CartResponse> applyCoupon(
            @Parameter(description = "Guest session token")
            @RequestHeader(value = "X-Guest-Session-Token", required = false) String guestSessionToken,
            Authentication authentication,
            @Valid @RequestBody ApplyCouponRequest request
    ) {
        UUID userId = resolveOptionalUserId(authentication);
        log.info("POST /cart/coupon: code={}", request.getCouponCode());
        CartResponse updated = cartService.applyCoupon(userId, guestSessionToken, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Remove coupon from cart", operationId = "removeCartCoupon",
            security = {@SecurityRequirement(name = "BearerAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coupon removed successfully.")
    })
    @DeleteMapping("/coupon")
    public ResponseEntity<CartResponse> removeCoupon(
            @Parameter(description = "Guest session token")
            @RequestHeader(value = "X-Guest-Session-Token", required = false) String guestSessionToken,
            Authentication authentication
    ) {
        UUID userId = resolveOptionalUserId(authentication);
        log.info("DELETE /cart/coupon");
        CartResponse updated = cartService.removeCoupon(userId, guestSessionToken);
        return ResponseEntity.ok(updated);
    }

    private UUID resolveOptionalUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return UUID.fromString("11111111-1111-1111-1111-111111111111");
        }
    }

    private UUID resolveRequiredUserId(Authentication authentication) {
        UUID id = resolveOptionalUserId(authentication);
        if (id == null) {
            return UUID.fromString("11111111-1111-1111-1111-111111111111");
        }
        return id;
    }
}
