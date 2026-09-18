package com.bookcorner.controller;

import com.bookcorner.common.exception.InsufficientStockException;
import com.bookcorner.dto.cart.AddCartItemRequest;
import com.bookcorner.dto.cart.ApplyCouponRequest;
import com.bookcorner.dto.cart.CartResponse;
import com.bookcorner.dto.cart.MergeCartRequest;
import com.bookcorner.dto.cart.UpdateCartItemQuantityRequest;
import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.security.CustomAccessDeniedHandler;
import com.bookcorner.security.CustomAuthenticationEntryPoint;
import com.bookcorner.security.CustomUserDetailsService;
import com.bookcorner.security.JwtFilter;
import com.bookcorner.security.JwtProvider;
import com.bookcorner.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CartController WebMvc Tests (MockMvc & AssertJ)")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Test
    @DisplayName("GET /cart should return 200 OK with active cart representation")
    void shouldGetActiveCartSuccessfully() throws Exception {
        CartResponse cartResponse = CartResponse.builder()
                .cartId(UUID.randomUUID())
                .currencyCode("USD")
                .subtotal(MoneyDto.builder().amount(4500L).currency("USD").build())
                .total(MoneyDto.builder().amount(4500L).currency("USD").build())
                .items(new ArrayList<>())
                .build();

        when(cartService.getCart(isNull(), eq("gst_session_token"))).thenReturn(cartResponse);

        mockMvc.perform(get("/cart")
                        .header("X-Guest-Session-Token", "gst_session_token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyCode").isEqualTo("USD"))
                .andExpect(jsonPath("$.subtotal.amount").isEqualTo(4500));
    }

    @Test
    @DisplayName("POST /cart/items should add format SKU and return 200 OK")
    void shouldAddItemToCartSuccessfully() throws Exception {
        UUID formatId = UUID.randomUUID();
        AddCartItemRequest request = AddCartItemRequest.builder()
                .formatId(formatId)
                .quantity(2)
                .build();

        CartResponse cartResponse = CartResponse.builder()
                .cartId(UUID.randomUUID())
                .currencyCode("USD")
                .subtotal(MoneyDto.builder().amount(8000L).currency("USD").build())
                .total(MoneyDto.builder().amount(8000L).currency("USD").build())
                .build();

        when(cartService.addItemToCart(isNull(), eq("gst_token"), isNull(), any(AddCartItemRequest.class)))
                .thenReturn(cartResponse);

        mockMvc.perform(post("/cart/items")
                        .header("X-Guest-Session-Token", "gst_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal.amount").isEqualTo(8000));
    }

    @Test
    @DisplayName("POST /cart/items should return 400 Bad Request when quantity is less than 1")
    void shouldRejectAddItemWithZeroQuantity() throws Exception {
        AddCartItemRequest request = AddCartItemRequest.builder()
                .formatId(UUID.randomUUID())
                .quantity(0) // Minimum is 1
                .build();

        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isEqualTo("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.invalidParams[0].name").isEqualTo("quantity"));
    }

    @Test
    @DisplayName("POST /cart/items should return 409 Conflict when stock is insufficient")
    void shouldReturn409WhenStockInsufficient() throws Exception {
        UUID formatId = UUID.randomUUID();
        AddCartItemRequest request = AddCartItemRequest.builder()
                .formatId(formatId)
                .quantity(5)
                .build();

        when(cartService.addItemToCart(isNull(), isNull(), isNull(), any(AddCartItemRequest.class)))
                .thenThrow(new InsufficientStockException("SKU-TEST-1", 5, 2));

        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").isEqualTo("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.detail").contains("Insufficient inventory"));
    }

    @Test
    @DisplayName("PATCH /cart/items/{formatId} should update line item quantity")
    void shouldUpdateCartItemQuantitySuccessfully() throws Exception {
        UUID formatId = UUID.randomUUID();
        UpdateCartItemQuantityRequest request = UpdateCartItemQuantityRequest.builder()
                .quantity(3)
                .build();

        CartResponse cartResponse = CartResponse.builder()
                .cartId(UUID.randomUUID())
                .subtotal(MoneyDto.builder().amount(9000L).currency("USD").build())
                .build();

        when(cartService.updateItemQuantity(isNull(), isNull(), eq(formatId), any(UpdateCartItemQuantityRequest.class)))
                .thenReturn(cartResponse);

        mockMvc.perform(patch("/cart/items/{formatId}", formatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal.amount").isEqualTo(9000));
    }

    @Test
    @DisplayName("DELETE /cart/items/{formatId} should remove item and return 204 No Content")
    void shouldRemoveCartItemSuccessfully() throws Exception {
        UUID formatId = UUID.randomUUID();

        mockMvc.perform(delete("/cart/items/{formatId}", formatId))
                .andExpect(status().isNoContent());

        verify(cartService).removeItemFromCart(isNull(), isNull(), eq(formatId));
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = {"CUSTOMER"})
    @DisplayName("POST /cart/merge should combine guest cart and return 200 OK")
    void shouldMergeGuestCartSuccessfully() throws Exception {
        MergeCartRequest request = MergeCartRequest.builder()
                .guestSessionToken("gst_merge_token")
                .build();

        CartResponse cartResponse = CartResponse.builder()
                .cartId(UUID.randomUUID())
                .subtotal(MoneyDto.builder().amount(15000L).currency("USD").build())
                .build();

        when(cartService.mergeGuestCart(any(UUID.class), eq("gst_merge_token"))).thenReturn(cartResponse);

        mockMvc.perform(post("/cart/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal.amount").isEqualTo(15000));
    }

    @Test
    @DisplayName("POST /cart/coupon should apply promotional code to cart")
    void shouldApplyCouponSuccessfully() throws Exception {
        ApplyCouponRequest request = ApplyCouponRequest.builder()
                .couponCode("SAVE15")
                .build();

        CartResponse cartResponse = CartResponse.builder()
                .cartId(UUID.randomUUID())
                .discount(MoneyDto.builder().amount(1500L).currency("USD").build())
                .build();

        when(cartService.applyCoupon(isNull(), isNull(), any(ApplyCouponRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(post("/cart/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discount.amount").isEqualTo(1500));
    }

    @Test
    @DisplayName("DELETE /cart/coupon should remove coupon and recalculate cart")
    void shouldRemoveCouponSuccessfully() throws Exception {
        CartResponse cartResponse = CartResponse.builder()
                .cartId(UUID.randomUUID())
                .discount(null)
                .build();

        when(cartService.removeCoupon(isNull(), isNull())).thenReturn(cartResponse);

        mockMvc.perform(delete("/cart/coupon"))
                .andExpect(status().isOk());

        verify(cartService).removeCoupon(isNull(), isNull());
    }
}
