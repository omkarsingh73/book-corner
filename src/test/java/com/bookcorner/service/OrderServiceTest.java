package com.bookcorner.service;

import com.bookcorner.common.exception.BusinessRuleViolationException;
import com.bookcorner.common.exception.InsufficientStockException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.common.AddressDto;
import com.bookcorner.dto.order.CancelOrderRequest;
import com.bookcorner.dto.order.CheckoutOrderRequest;
import com.bookcorner.dto.order.OrderCancellationResponse;
import com.bookcorner.dto.order.OrderConfirmationResponse;
import com.bookcorner.dto.order.OrderDetailResponse;
import com.bookcorner.dto.order.OrderSummaryDto;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.entity.member.UserAddressEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.ordering.CartEntity;
import com.bookcorner.entity.ordering.CartItemEntity;
import com.bookcorner.entity.ordering.CouponEntity;
import com.bookcorner.entity.ordering.OrderEntity;
import com.bookcorner.entity.ordering.OrderLineItemEntity;
import com.bookcorner.entity.ordering.ReturnRequestEntity;
import com.bookcorner.mapper.OrderMapper;
import com.bookcorner.repository.catalog.BookFormatRepository;
import com.bookcorner.repository.member.UserAddressRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.ordering.OrderRepository;
import com.bookcorner.repository.ordering.ReturnRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests (Mockito & AssertJ)")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private BookFormatRepository bookFormatRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartService cartService;

    @Mock
    private CouponService couponService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ShippingService shippingService;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private OrderService orderService;

    private UUID userId;
    private UserEntity user;
    private BookFormatEntity format1;
    private BookFormatEntity format2;
    private CartEntity cart;
    private AddressDto addressDto;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        lenient().doAnswer(invocation -> {
            java.util.function.Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        userId = UUID.randomUUID();

        user = UserEntity.builder()
                .id(userId)
                .email("testbuyer@example.com")
                .firstName("John")
                .lastName("Doe")
                .accountStatus("ACTIVE")
                .build();

        BookEntity book1 = BookEntity.builder()
                .id(UUID.randomUUID())
                .title("Design Patterns")
                .isbn13("9780201633610")
                .build();

        format1 = BookFormatEntity.builder()
                .id(UUID.randomUUID())
                .book(book1)
                .sku("SKU-DP-HC")
                .formatType("HARDCOVER")
                .basePriceAmount(4500L)
                .currencyCode("USD")
                .stockQuantity(15)
                .build();

        BookEntity book2 = BookEntity.builder()
                .id(UUID.randomUUID())
                .title("Refactoring")
                .isbn13("9780134757599")
                .build();

        format2 = BookFormatEntity.builder()
                .id(UUID.randomUUID())
                .book(book2)
                .sku("SKU-RF-PB")
                .formatType("PAPERBACK")
                .basePriceAmount(3500L)
                .currencyCode("USD")
                .stockQuantity(20)
                .build();

        cart = CartEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .storeId(UUID.randomUUID())
                .items(new ArrayList<>())
                .build();

        CartItemEntity item1 = CartItemEntity.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .format(format1)
                .quantity(1)
                .build();

        CartItemEntity item2 = CartItemEntity.builder()
                .id(UUID.randomUUID())
                .cart(cart)
                .format(format2)
                .quantity(2)
                .build();

        cart.getItems().add(item1);
        cart.getItems().add(item2);

        addressDto = AddressDto.builder()
                .recipientName("John Doe")
                .streetLine1("123 Book Lane")
                .city("San Francisco")
                .stateOrProvince("CA")
                .postalCode("94105")
                .countryCode("USA")
                .phoneNumber("+15551234567")
                .build();
    }

    @Test
    @DisplayName("Should successfully checkout cart, process payment, and return order confirmation")
    void shouldCheckoutSuccessfully() {
        CheckoutOrderRequest request = CheckoutOrderRequest.builder()
                .newShippingAddress(addressDto)
                .paymentMethodToken("pm_card_visa")
                .couponCode("SAVE10")
                .build();

        CouponEntity coupon = CouponEntity.builder()
                .id(UUID.randomUUID())
                .couponCode("SAVE10")
                .discountType("PERCENTAGE")
                .discountValue(10L)
                .isActive(true)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getCartEntityForCheckout(userId)).thenReturn(cart);
        when(bookFormatRepository.decrementInventory(eq(format1.getId()), eq(1))).thenReturn(1);
        when(bookFormatRepository.decrementInventory(eq(format2.getId()), eq(2))).thenReturn(1);
        when(couponService.validateCoupon(eq("SAVE10"), anyLong())).thenReturn(coupon);
        when(couponService.calculateDiscount(eq(coupon), anyLong())).thenReturn(1150L);

        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderConfirmationResponse expectedResponse = OrderConfirmationResponse.builder()
                .orderNumber("ORD-TEST-1234")
                .orderStatus("CONFIRMED")
                .build();
        when(orderMapper.toOrderConfirmationResponse(any(OrderEntity.class))).thenReturn(expectedResponse);

        OrderConfirmationResponse response = orderService.checkout(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderStatus()).isEqualTo("CONFIRMED");

        verify(paymentService).processOrderPayment(any(OrderEntity.class), eq("pm_card_visa"), anyString());
        verify(shippingService).createConsignment(any(OrderEntity.class), eq("FEDEX"), eq("Standard Ground"), anyLong());
        verify(cartService).clearCart(cart.getId());
        verify(couponService).redeemCoupon(coupon.getId());
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when checking out empty cart")
    void shouldThrowExceptionWhenCheckingOutEmptyCart() {
        cart.getItems().clear();

        CheckoutOrderRequest request = CheckoutOrderRequest.builder()
                .newShippingAddress(addressDto)
                .paymentMethodToken("pm_card_visa")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getCartEntityForCheckout(userId)).thenReturn(cart);

        assertThatThrownBy(() -> orderService.checkout(userId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Cannot checkout an empty shopping cart");

        verify(paymentService, never()).processOrderPayment(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw InsufficientStockException and rollback reserved inventory on stock failure")
    void shouldThrowInsufficientStockExceptionAndCompensate() {
        CheckoutOrderRequest request = CheckoutOrderRequest.builder()
                .newShippingAddress(addressDto)
                .paymentMethodToken("pm_card_visa")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getCartEntityForCheckout(userId)).thenReturn(cart);
        // Format 1 reserves successfully
        when(bookFormatRepository.decrementInventory(eq(format1.getId()), eq(1))).thenReturn(1);
        // Format 2 fails (out of stock)
        when(bookFormatRepository.decrementInventory(eq(format2.getId()), eq(2))).thenReturn(0);

        assertThatThrownBy(() -> orderService.checkout(userId, request))
                .isInstanceOf(InsufficientStockException.class);

        // Verify compensation: format 1 incremented back
        verify(bookFormatRepository).incrementInventory(eq(format1.getId()), eq(1));
        verify(orderRepository, never()).save(any());
        verify(paymentService, never()).processOrderPayment(any(), any(), any());
    }

    @Test
    @DisplayName("Should compensate inventory and mark order FAILED when payment processing fails")
    void shouldCompensateAndMarkFailedWhenPaymentFails() {
        CheckoutOrderRequest request = CheckoutOrderRequest.builder()
                .newShippingAddress(addressDto)
                .paymentMethodToken("pm_bad_card")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartService.getCartEntityForCheckout(userId)).thenReturn(cart);
        when(bookFormatRepository.decrementInventory(eq(format1.getId()), eq(1))).thenReturn(1);
        when(bookFormatRepository.decrementInventory(eq(format2.getId()), eq(2))).thenReturn(1);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(paymentService.processOrderPayment(any(OrderEntity.class), eq("pm_bad_card"), anyString()))
                .thenThrow(new RuntimeException("Payment card declined: insufficient funds"));

        assertThatThrownBy(() -> orderService.checkout(userId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Payment processing failed");

        // Verify all reserved items restocked
        verify(bookFormatRepository).incrementInventory(eq(format1.getId()), eq(1));
        verify(bookFormatRepository).incrementInventory(eq(format2.getId()), eq(2));
        verify(cartService, never()).clearCart(any());
    }

    @Test
    @DisplayName("Should retrieve order details for authorized user")
    void shouldGetOrderDetailsSuccessfully() {
        String orderNumber = "ORD-20260918-ABCD";
        OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .orderNumber(orderNumber)
                .user(user)
                .orderStatus("CONFIRMED")
                .totalAmount(11500L)
                .build();

        when(orderRepository.findByOrderNumberWithDetails(orderNumber)).thenReturn(Optional.of(order));

        OrderDetailResponse detailResponse = OrderDetailResponse.builder()
                .orderNumber(orderNumber)
                .orderStatus("CONFIRMED")
                .build();
        when(orderMapper.toOrderDetailResponse(order)).thenReturn(detailResponse);

        OrderDetailResponse result = orderService.getOrderDetails(userId, orderNumber);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo(orderNumber);
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when user tries to view someone else's order")
    void shouldThrowExceptionWhenUserNotAuthorizedForOrder() {
        String orderNumber = "ORD-20260918-XYZ";
        UserEntity otherUser = UserEntity.builder().id(UUID.randomUUID()).build();
        OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .orderNumber(orderNumber)
                .user(otherUser)
                .build();

        when(orderRepository.findByOrderNumberWithDetails(orderNumber)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderDetails(userId, orderNumber))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("You are not authorized to view this order");
    }

    @Test
    @DisplayName("Should cancel order, restock inventory, and trigger payment refund")
    void shouldCancelOrderSuccessfully() {
        String orderNumber = "ORD-20260918-CANCEL";
        OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .orderNumber(orderNumber)
                .user(user)
                .orderStatus("CONFIRMED")
                .totalAmount(8000L)
                .lineItems(new ArrayList<>())
                .statusHistory(new ArrayList<>())
                .build();

        OrderLineItemEntity lineItem = OrderLineItemEntity.builder()
                .id(UUID.randomUUID())
                .order(order)
                .format(format1)
                .quantity(2)
                .build();
        order.getLineItems().add(lineItem);

        CancelOrderRequest request = CancelOrderRequest.builder()
                .reason("Ordered by mistake")
                .build();

        when(orderRepository.findByOrderNumberWithDetails(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCancellationResponse response = OrderCancellationResponse.builder()
                .orderNumber(orderNumber)
                .cancellationStatus("CANCELLED")
                .refundInitiated(true)
                .build();
        when(orderMapper.toOrderCancellationResponse(any(OrderEntity.class))).thenReturn(response);

        OrderCancellationResponse result = orderService.cancelOrder(userId, orderNumber, request);

        assertThat(result).isNotNull();
        assertThat(result.getCancellationStatus()).isEqualTo("CANCELLED");

        verify(bookFormatRepository).incrementInventory(eq(format1.getId()), eq(2));
        verify(paymentService).processRefund(order.getId(), 8000L, "Ordered by mistake");
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when attempting to cancel shipped order")
    void shouldThrowExceptionWhenCancellingShippedOrder() {
        String orderNumber = "ORD-20260918-SHIPPED";
        OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .orderNumber(orderNumber)
                .user(user)
                .orderStatus("SHIPPED")
                .build();

        CancelOrderRequest request = CancelOrderRequest.builder()
                .reason("Changed my mind")
                .build();

        when(orderRepository.findByOrderNumberWithDetails(orderNumber)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(userId, orderNumber, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot be cancelled");

        verify(paymentService, never()).processRefund(any(), anyLong(), any());
    }

    @Test
    @DisplayName("Should list orders with pagination")
    void shouldListOrdersPaginated() {
        Pageable pageable = PageRequest.of(0, 10);
        OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-1")
                .user(user)
                .build();
        Page<OrderEntity> page = new PageImpl<>(List.of(order), pageable, 1);

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);
        when(orderMapper.toOrderSummaryDto(order)).thenReturn(OrderSummaryDto.builder().orderNumber("ORD-1").build());

        Page<OrderSummaryDto> result = orderService.listOrders(userId, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ORD-1");
    }

    @Test
    @DisplayName("Should submit return request for confirmed/delivered order")
    void shouldSubmitReturnRequestSuccessfully() {
        String orderNumber = "ORD-20260918-DELIVERED";
        OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .orderNumber(orderNumber)
                .user(user)
                .orderStatus("DELIVERED")
                .totalAmount(5000L)
                .lineItems(new ArrayList<>())
                .build();

        OrderLineItemEntity lineItem = OrderLineItemEntity.builder()
                .id(UUID.randomUUID())
                .order(order)
                .format(format1)
                .quantity(1)
                .build();
        order.getLineItems().add(lineItem);

        when(orderRepository.findByOrderNumberWithDetails(orderNumber)).thenReturn(Optional.of(order));
        when(returnRequestRepository.save(any(ReturnRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnRequestEntity rma = orderService.requestReturn(userId, orderNumber, "DAMAGED_ITEM", "Cover torn");

        assertThat(rma).isNotNull();
        assertThat(rma.getReasonCode()).isEqualTo("DAMAGED_ITEM");
        assertThat(rma.getReturnStatus()).isEqualTo("REQUESTED");
        assertThat(rma.getRefundAmount()).isEqualTo(5000L);
        verify(returnRequestRepository).save(any(ReturnRequestEntity.class));
    }
}
