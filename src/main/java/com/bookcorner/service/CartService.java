package com.bookcorner.service;

import com.bookcorner.common.exception.BusinessRuleViolationException;
import com.bookcorner.common.exception.InsufficientStockException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.cart.AddCartItemRequest;
import com.bookcorner.dto.cart.ApplyCouponRequest;
import com.bookcorner.dto.cart.CartResponse;
import com.bookcorner.dto.cart.UpdateCartItemQuantityRequest;
import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.entity.member.GuestSessionEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.ordering.CartEntity;
import com.bookcorner.entity.ordering.CartItemEntity;
import com.bookcorner.entity.ordering.CouponEntity;
import com.bookcorner.entity.store.StoreEntity;
import com.bookcorner.mapper.CartMapper;
import com.bookcorner.repository.catalog.BookFormatRepository;
import com.bookcorner.repository.member.GuestSessionRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.ordering.CartItemRepository;
import com.bookcorner.repository.ordering.CartRepository;
import com.bookcorner.repository.store.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Shopping Cart and Basket Lifecycle Service.
 * Manages persistent member carts, anonymous guest baskets, stock validations,
 * line-item quantity mutations, and guest-to-account cart merging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookFormatRepository bookFormatRepository;
    private final UserRepository userRepository;
    private final GuestSessionRepository guestSessionRepository;
    private final StoreRepository storeRepository;
    private final CouponService couponService;
    private final CartMapper cartMapper;

    /**
     * Retrieves or initializes an active shopping cart for an authenticated user or guest session.
     */
    @Transactional
    public CartResponse getCart(UUID userId, String guestSessionToken) {
        log.info("Fetching cart for user: {}, guestToken: {}", userId, guestSessionToken != null ? "[PROTECTED]" : "null");
        CartEntity cart = resolveOrCreateCart(userId, guestSessionToken, null);
        return buildCartResponse(cart);
    }

    /**
     * Adds a book format SKU line item to the customer or guest shopping cart.
     */
    @Transactional
    public CartResponse addItemToCart(UUID userId, String guestSessionToken, UUID storeId, AddCartItemRequest request) {
        log.info("Adding SKU format {} (qty: {}) to cart. User: {}, GuestToken present: {}",
                request.getFormatId(), request.getQuantity(), userId, guestSessionToken != null);

        BookFormatEntity format = bookFormatRepository.findById(request.getFormatId())
                .orElseThrow(() -> new ResourceNotFoundException("Book format not found for ID: " + request.getFormatId()));

        if (!format.isActive()) {
            throw new BusinessRuleViolationException("The requested book format is no longer available for purchase.");
        }

        if (format.getInventoryQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(format.getSku(), request.getQuantity(), format.getInventoryQuantity());
        }

        CartEntity cart = resolveOrCreateCart(userId, guestSessionToken, storeId);

        Optional<CartItemEntity> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getFormat().getId().equals(format.getId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItemEntity existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (newQuantity > 99) {
                throw new BusinessRuleViolationException("Maximum quantity allowed per line item is 99.");
            }
            if (format.getInventoryQuantity() < newQuantity) {
                throw new InsufficientStockException(format.getSku(), newQuantity, format.getInventoryQuantity());
            }

            existingItem.setQuantity(newQuantity);
            log.info("Updated existing line item {} quantity to {}", existingItem.getId(), newQuantity);
        } else {
            CartItemEntity newItem = CartItemEntity.builder()
                    .cart(cart)
                    .format(format)
                    .quantity(request.getQuantity())
                    .build();
            cart.addItem(newItem);
            log.info("Added new item to cart {}: format {}", cart.getId(), format.getId());
        }

        validateAndReapplyCoupon(cart);
        CartEntity savedCart = cartRepository.save(cart);
        return buildCartResponse(savedCart);
    }

    /**
     * Mutates the quantity of an existing line item in the shopping cart.
     */
    @Transactional
    public CartResponse updateItemQuantity(UUID userId, String guestSessionToken, UUID formatId, UpdateCartItemQuantityRequest request) {
        log.info("Updating line item format {} quantity to {} in cart", formatId, request.getQuantity());

        CartEntity cart = getExistingCart(userId, guestSessionToken);

        CartItemEntity item = cart.getItems().stream()
                .filter(i -> i.getFormat().getId().equals(formatId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Format " + formatId + " not found in current cart."));

        if (request.getQuantity() <= 0) {
            cart.removeItem(item);
            cartItemRepository.delete(item);
            log.info("Removed format {} from cart {} due to zero quantity", formatId, cart.getId());
        } else {
            BookFormatEntity format = item.getFormat();
            if (format.getInventoryQuantity() < request.getQuantity()) {
                throw new InsufficientStockException(format.getSku(), request.getQuantity(), format.getInventoryQuantity());
            }
            item.setQuantity(request.getQuantity());
        }

        validateAndReapplyCoupon(cart);
        CartEntity savedCart = cartRepository.save(cart);
        return buildCartResponse(savedCart);
    }

    /**
     * Removes a line item completely from the shopping cart.
     */
    @Transactional
    public CartResponse removeItemFromCart(UUID userId, String guestSessionToken, UUID formatId) {
        log.info("Removing format {} from cart", formatId);

        CartEntity cart = getExistingCart(userId, guestSessionToken);

        CartItemEntity item = cart.getItems().stream()
                .filter(i -> i.getFormat().getId().equals(formatId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Format " + formatId + " not found in current cart."));

        cart.removeItem(item);
        cartItemRepository.delete(item);

        validateAndReapplyCoupon(cart);
        CartEntity savedCart = cartRepository.save(cart);
        return buildCartResponse(savedCart);
    }

    /**
     * Applies a promotional coupon discount code to the cart.
     */
    @Transactional
    public CartResponse applyCoupon(UUID userId, String guestSessionToken, ApplyCouponRequest request) {
        log.info("Applying coupon: {} to cart", request.getCouponCode());

        CartEntity cart = getExistingCart(userId, guestSessionToken);
        if (cart.getItems().isEmpty()) {
            throw new BusinessRuleViolationException("Cannot apply coupon to an empty cart.");
        }

        long subtotalCents = calculateCartSubtotal(cart);
        CouponEntity coupon = couponService.validateCoupon(request.getCouponCode(), subtotalCents);

        cart.setAppliedCoupon(coupon);
        CartEntity savedCart = cartRepository.save(cart);
        log.info("Successfully applied coupon {} to cart {}", coupon.getCouponCode(), cart.getId());

        return buildCartResponse(savedCart);
    }

    /**
     * Removes the currently applied promotional coupon from the cart.
     */
    @Transactional
    public CartResponse removeCoupon(UUID userId, String guestSessionToken) {
        log.info("Removing applied coupon from cart");

        CartEntity cart = getExistingCart(userId, guestSessionToken);
        cart.setAppliedCoupon(null);
        CartEntity savedCart = cartRepository.save(cart);

        return buildCartResponse(savedCart);
    }

    /**
     * Merges an anonymous guest session basket into the authenticated user's cart upon login/registration.
     */
    @Transactional
    public CartResponse mergeGuestCart(UUID userId, String guestSessionToken) {
        if (guestSessionToken == null || guestSessionToken.isBlank()) {
            return getCart(userId, null);
        }

        log.info("Merging guest basket (token: [PROTECTED]) into user {} cart", userId);

        Optional<CartEntity> guestCartOpt = cartRepository.findByGuestSessionToken(guestSessionToken);
        if (guestCartOpt.isEmpty() || guestCartOpt.get().getItems().isEmpty()) {
            log.info("No guest cart items found to merge.");
            return getCart(userId, null);
        }

        CartEntity guestCart = guestCartOpt.get();
        CartEntity userCart = resolveOrCreateCart(userId, null, guestCart.getStoreId());

        for (CartItemEntity guestItem : new ArrayList<>(guestCart.getItems())) {
            Optional<CartItemEntity> existingUserItem = userCart.getItems().stream()
                    .filter(ui -> ui.getFormat().getId().equals(guestItem.getFormat().getId()))
                    .findFirst();

            if (existingUserItem.isPresent()) {
                CartItemEntity ui = existingUserItem.get();
                int combinedQty = Math.min(99, ui.getQuantity() + guestItem.getQuantity());
                int maxAvailable = guestItem.getFormat().getInventoryQuantity();
                ui.setQuantity(Math.min(combinedQty, Math.max(1, maxAvailable)));
            } else {
                CartItemEntity newUi = CartItemEntity.builder()
                        .cart(userCart)
                        .format(guestItem.getFormat())
                        .quantity(Math.min(guestItem.getQuantity(), guestItem.getFormat().getInventoryQuantity()))
                        .build();
                userCart.addItem(newUi);
            }
        }

        if (userCart.getAppliedCoupon() == null && guestCart.getAppliedCoupon() != null) {
            userCart.setAppliedCoupon(guestCart.getAppliedCoupon());
        }

        validateAndReapplyCoupon(userCart);

        // Remove guest cart items and guest cart
        guestCart.getItems().clear();
        cartRepository.delete(guestCart);
        log.info("Guest cart {} deleted and merged into user cart {}", guestCart.getId(), userCart.getId());

        CartEntity savedCart = cartRepository.save(userCart);
        return buildCartResponse(savedCart);
    }

    /**
     * Clears all items and promotional discounts from a cart.
     */
    @Transactional
    public void clearCart(UUID cartId) {
        log.info("Clearing items from cart ID: {}", cartId);
        cartItemRepository.deleteAllByCartId(cartId);
        cartRepository.findById(cartId).ifPresent(cart -> {
            cart.getItems().clear();
            cart.setAppliedCoupon(null);
            cartRepository.save(cart);
        });
    }

    /**
     * Retrieves cart aggregate for checkout processing, verifying basket is not empty.
     */
    @Transactional(readOnly = true)
    public CartEntity getCartEntityForCheckout(UUID userId) {
        CartEntity cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Active shopping cart not found for user: " + userId));

        if (cart.getItems().isEmpty()) {
            throw new BusinessRuleViolationException("Cannot checkout an empty shopping cart.");
        }

        return cart;
    }

    // --- Private Helper Methods ---

    private CartEntity getExistingCart(UUID userId, String guestSessionToken) {
        if (userId != null) {
            return cartRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found for authenticated user."));
        } else if (guestSessionToken != null && !guestSessionToken.isBlank()) {
            return cartRepository.findByGuestSessionToken(guestSessionToken)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found for guest session."));
        }
        throw new BusinessRuleViolationException("Either authenticated user ID or guest session token must be provided.");
    }

    private CartEntity resolveOrCreateCart(UUID userId, String guestSessionToken, UUID storeId) {
        if (userId != null) {
            return cartRepository.findByUserId(userId)
                    .orElseGet(() -> createCartForUser(userId, storeId));
        } else if (guestSessionToken != null && !guestSessionToken.isBlank()) {
            return cartRepository.findByGuestSessionToken(guestSessionToken)
                    .orElseGet(() -> createCartForGuest(guestSessionToken, storeId));
        }
        throw new BusinessRuleViolationException("Either authenticated user ID or guest session token must be provided.");
    }

    private CartEntity createCartForUser(UUID userId, UUID storeId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        UUID effectiveStoreId = resolveStoreId(storeId);
        CartEntity newCart = CartEntity.builder()
                .user(user)
                .storeId(effectiveStoreId)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();

        log.info("Creating fresh shopping cart for user {}", userId);
        return cartRepository.save(newCart);
    }

    private CartEntity createCartForGuest(String guestSessionToken, UUID storeId) {
        GuestSessionEntity guestSession = guestSessionRepository.findBySessionToken(guestSessionToken)
                .orElseThrow(() -> new ResourceNotFoundException("Guest session expired or not found."));

        UUID effectiveStoreId = storeId != null ? storeId : guestSession.getStoreId();
        effectiveStoreId = resolveStoreId(effectiveStoreId);

        CartEntity newCart = CartEntity.builder()
                .guestSession(guestSession)
                .storeId(effectiveStoreId)
                .expiresAt(Instant.now().plus(14, ChronoUnit.DAYS))
                .build();

        log.info("Creating fresh shopping cart for guest token: [PROTECTED]");
        return cartRepository.save(newCart);
    }

    private UUID resolveStoreId(UUID storeId) {
        if (storeId != null) {
            return storeId;
        }
        return storeRepository.findAll().stream()
                .findFirst()
                .map(StoreEntity::getId)
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    private long calculateCartSubtotal(CartEntity cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return 0L;
        }
        return cart.getItems().stream()
                .mapToLong(item -> item.getFormat().getBasePriceAmount() * item.getQuantity())
                .sum();
    }

    private void validateAndReapplyCoupon(CartEntity cart) {
        if (cart.getAppliedCoupon() == null) {
            return;
        }
        long subtotalCents = calculateCartSubtotal(cart);
        try {
            couponService.validateCoupon(cart.getAppliedCoupon().getCouponCode(), subtotalCents);
        } catch (Exception e) {
            log.warn("Applied coupon {} is no longer valid after cart change: {}",
                    cart.getAppliedCoupon().getCouponCode(), e.getMessage());
            cart.setAppliedCoupon(null);
        }
    }

    private CartResponse buildCartResponse(CartEntity cart) {
        CartResponse response = cartMapper.toCartResponse(cart);
        long subtotalCents = calculateCartSubtotal(cart);
        String currency = (!cart.getItems().isEmpty() && cart.getItems().get(0).getFormat() != null)
                ? cart.getItems().get(0).getFormat().getCurrencyCode()
                : "USD";

        long discountCents = 0L;
        if (cart.getAppliedCoupon() != null) {
            discountCents = couponService.calculateDiscount(cart.getAppliedCoupon(), subtotalCents);
            response.setCouponCode(cart.getAppliedCoupon().getCouponCode());
        }

        // Free shipping over $50.00 (5000 cents), otherwise $4.99 (499 cents)
        long shippingCents = (subtotalCents >= 5000L || subtotalCents == 0L) ? 0L : 499L;

        // Estimated sales tax: 8% on discounted subtotal
        long taxableAmount = Math.max(0L, subtotalCents - discountCents);
        long taxCents = (long) (taxableAmount * 0.08);

        long totalCents = taxableAmount + shippingCents + taxCents;

        response.setSubtotal(MoneyDto.of(subtotalCents, currency));
        response.setDiscount(MoneyDto.of(discountCents, currency));
        response.setEstimatedShipping(MoneyDto.of(shippingCents, currency));
        response.setEstimatedTax(MoneyDto.of(taxCents, currency));
        response.setTotal(MoneyDto.of(totalCents, currency));

        return response;
    }
}
