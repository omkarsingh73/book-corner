package com.bookcorner.controller;

import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.common.AddressDto;
import com.bookcorner.dto.order.CancelOrderRequest;
import com.bookcorner.dto.order.CheckoutOrderRequest;
import com.bookcorner.dto.order.CreateReturnRequest;
import com.bookcorner.dto.order.OrderCancellationResponse;
import com.bookcorner.dto.order.OrderConfirmationResponse;
import com.bookcorner.dto.order.OrderDetailResponse;
import com.bookcorner.dto.order.OrderSummaryDto;
import com.bookcorner.entity.ordering.ReturnRequestEntity;
import com.bookcorner.security.CustomAccessDeniedHandler;
import com.bookcorner.security.CustomAuthenticationEntryPoint;
import com.bookcorner.security.CustomUserDetailsService;
import com.bookcorner.security.JwtFilter;
import com.bookcorner.security.JwtProvider;
import com.bookcorner.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController WebMvc Tests (MockMvc & AssertJ)")
class OrderControllerTest {

    private static final String MOCK_USER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

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
    @WithMockUser(username = MOCK_USER_ID, roles = {"CUSTOMER"})
    @DisplayName("POST /orders/checkout should place order and return 201 Created")
    void shouldCheckoutOrderSuccessfully() throws Exception {
        AddressDto shipping = AddressDto.builder()
                .recipientName("John Watson")
                .streetLine1("221B Baker St")
                .city("London")
                .stateOrProvince("Greater London")
                .postalCode("NW1 6XE")
                .countryCode("GBR")
                .build();

        CheckoutOrderRequest request = CheckoutOrderRequest.builder()
                .newShippingAddress(shipping)
                .paymentMethodToken("tok_visa_valid")
                .couponCode("DISCOUNT10")
                .build();

        OrderConfirmationResponse response = OrderConfirmationResponse.builder()
                .orderNumber("ORD-20260918-9999")
                .orderStatus("CONFIRMED")
                .build();

        when(orderService.checkout(any(UUID.class), any(CheckoutOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/orders/checkout")
                        .header("Idempotency-Key", "idemp_key_123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").isEqualTo("ORD-20260918-9999"))
                .andExpect(jsonPath("$.orderStatus").isEqualTo("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /orders/checkout should return 403 Forbidden when unauthenticated")
    void shouldRejectCheckoutWithoutAuthentication() throws Exception {
        CheckoutOrderRequest request = CheckoutOrderRequest.builder()
                .paymentMethodToken("tok_visa")
                .build();

        mockMvc.perform(post("/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").isEqualTo("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = MOCK_USER_ID, roles = {"CUSTOMER"})
    @DisplayName("POST /orders/checkout should return 400 Bad Request when payment token is blank")
    void shouldRejectCheckoutWithMissingPaymentToken() throws Exception {
        CheckoutOrderRequest request = CheckoutOrderRequest.builder()
                .paymentMethodToken("") // Blank payment token
                .build();

        mockMvc.perform(post("/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isEqualTo("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.invalidParams[0].name").isEqualTo("paymentMethodToken"));
    }

    @Test
    @WithMockUser(username = MOCK_USER_ID, roles = {"CUSTOMER"})
    @DisplayName("GET /orders should return 200 OK with paginated orders")
    void shouldListOrdersSuccessfully() throws Exception {
        OrderSummaryDto summary = OrderSummaryDto.builder()
                .orderNumber("ORD-1")
                .orderStatus("CONFIRMED")
                .build();
        Page<OrderSummaryDto> page = new PageImpl<>(List.of(summary));

        when(orderService.listOrders(any(UUID.class), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/orders")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].orderNumber").isEqualTo("ORD-1"));
    }

    @Test
    @WithMockUser(username = MOCK_USER_ID, roles = {"CUSTOMER"})
    @DisplayName("GET /orders/{orderNumber} should return 200 OK with order details")
    void shouldGetOrderByNumberSuccessfully() throws Exception {
        OrderDetailResponse detail = OrderDetailResponse.builder()
                .orderNumber("ORD-1001")
                .orderStatus("DELIVERED")
                .build();

        when(orderService.getOrderDetails(any(UUID.class), eq("ORD-1001"))).thenReturn(detail);

        mockMvc.perform(get("/orders/ORD-1001")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").isEqualTo("ORD-1001"))
                .andExpect(jsonPath("$.orderStatus").isEqualTo("DELIVERED"));
    }

    @Test
    @WithMockUser(username = MOCK_USER_ID, roles = {"CUSTOMER"})
    @DisplayName("GET /orders/{orderNumber} should return 404 Not Found when order is absent")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        when(orderService.getOrderDetails(any(UUID.class), eq("ORD-MISSING")))
                .thenThrow(new ResourceNotFoundException("Order not found: ORD-MISSING"));

        mockMvc.perform(get("/orders/ORD-MISSING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = MOCK_USER_ID, roles = {"CUSTOMER"})
    @DisplayName("POST /orders/{orderNumber}/cancel should cancel order and return 200 OK")
    void shouldCancelOrderSuccessfully() throws Exception {
        CancelOrderRequest request = CancelOrderRequest.builder()
                .reason("Customer changed mind")
                .build();

        OrderCancellationResponse response = OrderCancellationResponse.builder()
                .orderNumber("ORD-CANCEL-1")
                .cancellationStatus("CANCELLED")
                .refundInitiated(true)
                .build();

        when(orderService.cancelOrder(any(UUID.class), eq("ORD-CANCEL-1"), any(CancelOrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/orders/ORD-CANCEL-1/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").isEqualTo("ORD-CANCEL-1"))
                .andExpect(jsonPath("$.cancellationStatus").isEqualTo("CANCELLED"))
                .andExpect(jsonPath("$.refundInitiated").isEqualTo(true));
    }

    @Test
    @WithMockUser(username = MOCK_USER_ID, roles = {"CUSTOMER"})
    @DisplayName("POST /orders/{orderNumber}/returns should submit RMA and return 201 Created")
    void shouldRequestOrderReturnSuccessfully() throws Exception {
        CreateReturnRequest request = CreateReturnRequest.builder()
                .reasonCode("DAMAGED_ITEM")
                .customerRemarks("Water damage on pages")
                .returnItems(List.of(
                        CreateReturnRequest.ReturnItemRequest.builder()
                                .orderLineItemId(UUID.randomUUID())
                                .quantity(1)
                                .build()
                ))
                .build();

        ReturnRequestEntity mockRma = ReturnRequestEntity.builder()
                .rmaNumber("RMA-20260918-001")
                .returnStatus("REQUESTED")
                .refundAmount(4500L)
                .build();

        when(orderService.requestReturn(any(UUID.class), eq("ORD-RETURN-1"), eq("DAMAGED_ITEM"), eq("Water damage on pages")))
                .thenReturn(mockRma);

        mockMvc.perform(post("/orders/ORD-RETURN-1/returns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rmaNumber").isEqualTo("RMA-20260918-001"))
                .andExpect(jsonPath("$.rmaStatus").isEqualTo("REQUESTED"))
                .andExpect(jsonPath("$.returnCarrier").isEqualTo("UPS"));
    }
}
