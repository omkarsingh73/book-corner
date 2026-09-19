package com.bookcorner.service;

import com.bookcorner.common.exception.BusinessRuleViolationException;
import com.bookcorner.common.exception.InsufficientStockException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.cart.AddCartItemRequest;
import com.bookcorner.dto.cart.CartResponse;
import com.bookcorner.dto.cart.UpdateCartItemQuantityRequest;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.ordering.CartEntity;
import com.bookcorner.entity.ordering.CartItemEntity;
import com.bookcorner.entity.store.StoreEntity;
import com.bookcorner.mapper.CartMapper;
import com.bookcorner.repository.catalog.BookFormatRepository;
import com.bookcorner.repository.member.GuestSessionRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.ordering.CartItemRepository;
import com.bookcorner.repository.ordering.CartRepository;
import com.bookcorner.repository.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartService Unit Tests (Mockito & AssertJ)")
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private BookFormatRepository bookFormatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuestSessionRepository guestSessionRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private CouponService couponService;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    private UUID userId;
    private UUID formatId;
    private UserEntity user;
    private StoreEntity store;
    private BookEntity book;
    private BookFormatEntity format;
    private CartEntity cart;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        formatId = UUID.randomUUID();

        user = UserEntity.builder()
                .id(userId)
                .email("shopper@example.com")
                .firstName("John")
                .lastName("Watson")
                .accountStatus("ACTIVE")
                .build();

        store = StoreEntity.builder()
                .id(UUID.randomUUID())
                .storeCode("BK_MAIN_ONLINE")
                .storeName("Book Corner Online")
                .defaultCurrency("USD")
                .build();

        book = BookEntity.builder()
                .id(UUID.randomUUID())
                .title("Clean Code")
                .isbn13("9780132350884")
                .build();

        format = BookFormatEntity.builder()
                .id(formatId)
                .book(book)
                .sku("SKU-CC-PB")
                .formatType("PAPERBACK")
                .basePriceAmount(3999L)
                .currencyCode("USD")
                .stockQuantity(10)
                .build();

        cart = CartEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .storeId(store.getId())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .items(new ArrayList<>())
                .build();

        lenient().when(cartMapper.toCartResponse(any())).thenAnswer(invocation -> {
            CartEntity c = invocation.getArgument(0);
            return CartResponse.builder()
                    .cartId(c.getId())
                    .items(new ArrayList<>())
                    .build();
        });
    }

    @Test
    @DisplayName("Should successfully add a format item to user cart")
    void shouldAddItemToCartSuccessfully() {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .formatId(formatId)
                .quantity(2)
                .build();

        when(bookFormatRepository.findById(formatId)).thenReturn(Optional.of(format));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.addItemToCart(userId, null, null, request);

        assertThat(response).isNotNull();
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.getItems().get(0).getFormat().getId()).isEqualTo(formatId);
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when requested quantity exceeds available stock")
    void shouldThrowInsufficientStockException() {
        format.setStockQuantity(2); // Only 2 in stock
        AddCartItemRequest request = AddCartItemRequest.builder()
                .formatId(formatId)
                .quantity(5) // Requesting 5
                .build();

        when(bookFormatRepository.findById(formatId)).thenReturn(Optional.of(format));

        assertThatThrownBy(() -> cartService.addItemToCart(userId, null, null, request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient inventory");
    }

    @Test
    @DisplayName("Should update line item quantity in cart")
    void shouldUpdateItemQuantitySuccessfully() {
        CartItemEntity item = CartItemEntity.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .format(format)
                .quantity(2)
                .build();
        cart.getItems().add(item);

        UpdateCartItemQuantityRequest request = UpdateCartItemQuantityRequest.builder()
                .quantity(4)
                .build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.updateItemQuantity(userId, null, formatId, request);

        assertThat(response).isNotNull();
        assertThat(item.getQuantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should remove line item from cart")
    void shouldRemoveItemFromCartSuccessfully() {
        CartItemEntity item = CartItemEntity.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .format(format)
                .quantity(2)
                .build();
        cart.getItems().add(item);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartResponse response = cartService.removeItemFromCart(userId, null, formatId);

        assertThat(response).isNotNull();
        assertThat(cart.getItems()).isEmpty();
        verify(cartItemRepository).delete(item);
    }

    @Test
    @DisplayName("Should merge anonymous guest cart into customer cart upon login")
    void shouldMergeGuestCartSuccessfully() {
        CartEntity guestCart = CartEntity.builder()
                .id(UUID.randomUUID())
                .storeId(store.getId())
                .items(new ArrayList<>())
                .build();

        CartItemEntity guestItem = CartItemEntity.builder()
                .id(UUID.randomUUID())
                .cart(guestCart)
                .format(format)
                .quantity(3)
                .build();
        guestCart.getItems().add(guestItem);

        when(cartRepository.findByGuestSessionToken("gst_test_token")).thenReturn(Optional.of(guestCart));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        cartService.mergeGuestCart(userId, "gst_test_token");

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
        verify(cartRepository).delete(guestCart);
    }
}
