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
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.entity.member.UserAddressEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.ordering.CartEntity;
import com.bookcorner.entity.ordering.CartItemEntity;
import com.bookcorner.entity.ordering.CouponEntity;
import com.bookcorner.entity.ordering.OrderEntity;
import com.bookcorner.entity.ordering.OrderLineItemEntity;
import com.bookcorner.entity.ordering.ReturnRequestEntity;
import com.bookcorner.entity.ordering.ReturnRequestItemEntity;
import com.bookcorner.mapper.OrderMapper;
import com.bookcorner.repository.catalog.BookFormatRepository;
import com.bookcorner.repository.member.UserAddressRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.ordering.OrderRepository;
import com.bookcorner.repository.ordering.ReturnRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order Fulfillment and Checkout Orchestration Service.
 * Implements the decoupled Checkout Saga:
 * 1. Validates active customer cart items and quantities.
 * 2. Locks and reserves inventory decrementing format stock balances (Atomic DB TX).
 * 3. Freezes immutable JSONB address snapshots and persists PENDING_PAYMENT order.
 * 4. Executes payment authorization and captures transaction OUTSIDE of DB transaction.
 * 5. Completes order (marks CONFIRMED, creates consignment, clears cart) in a separate DB TX.
 * 6. Executes compensating restock and marks FAILED in a dedicated DB TX upon payment failure.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final BookFormatRepository bookFormatRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final CouponService couponService;
    private final PaymentService paymentService;
    private final ShippingService shippingService;
    private final OrderMapper orderMapper;
    private final TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Executes decoupled checkout saga for the customer's active cart.
     * Local database state transitions are decoupled from external network calls
     * to avoid holding HikariCP database connections during payment gateway I/O.
     */
    public OrderConfirmationResponse checkout(UUID userId, CheckoutOrderRequest request) {
        log.info("Initiating decoupled checkout saga for customer ID: {}", userId);

        // Phase 1: Atomically validate, reserve inventory, and persist Order in PENDING_PAYMENT
        OrderEntity pendingOrder = transactionTemplate.execute(status -> {
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

            CartEntity cart = cartService.getCartEntityForCheckout(userId);
            if (cart.getItems().isEmpty()) {
                throw new BusinessRuleViolationException("Cannot checkout an empty shopping cart.");
            }

            // 1. Resolve and serialize immutable address snapshots
            String shippingAddressJson = resolveShippingAddressJson(userId, request);
            String billingAddressJson = resolveBillingAddressJson(userId, request, shippingAddressJson);

            // 2. Validate format stock availability and atomically reserve inventory
            List<BookFormatEntity> reservedFormats = new ArrayList<>();
            for (CartItemEntity cartItem : cart.getItems()) {
                BookFormatEntity format = cartItem.getFormat();
                int rowsUpdated = bookFormatRepository.decrementInventory(format.getId(), cartItem.getQuantity());
                if (rowsUpdated == 0) {
                    for (BookFormatEntity reserved : reservedFormats) {
                        CartItemEntity matching = cart.getItems().stream()
                                .filter(ci -> ci.getFormat().getId().equals(reserved.getId()))
                                .findFirst().orElse(null);
                        if (matching != null) {
                            bookFormatRepository.incrementInventory(reserved.getId(), matching.getQuantity());
                        }
                    }
                    log.warn("Insufficient stock for SKU {} during checkout", format.getSku());
                    throw new InsufficientStockException(format.getSku(), cartItem.getQuantity(), format.getInventoryQuantity());
                }
                reservedFormats.add(format);
            }

            // 3. Calculate pricing breakdown
            long subtotalCents = cart.getItems().stream()
                    .mapToLong(item -> item.getFormat().getBasePriceAmount() * item.getQuantity())
                    .sum();

            CouponEntity coupon = null;
            if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
                coupon = couponService.validateCoupon(request.getCouponCode(), subtotalCents);
            } else if (cart.getAppliedCoupon() != null) {
                coupon = cart.getAppliedCoupon();
            }

            long discountCents = (coupon != null) ? couponService.calculateDiscount(coupon, subtotalCents) : 0L;
            long shippingCents = (subtotalCents >= 5000L) ? 0L : 499L; // Free shipping threshold $50.00
            long taxableCents = Math.max(0L, subtotalCents - discountCents);
            long taxCents = (long) (taxableCents * 0.08); // 8% sales tax estimate
            long totalCents = taxableCents + shippingCents + taxCents;

            String currency = cart.getItems().get(0).getFormat().getCurrencyCode();
            String orderNumber = "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // 4. Create and persist Order aggregate in PENDING_PAYMENT status
            OrderEntity order = OrderEntity.builder()
                    .orderNumber(orderNumber)
                    .user(user)
                    .storeId(cart.getStoreId())
                    .coupon(coupon)
                    .orderStatus("PENDING_PAYMENT")
                    .subtotalAmount(subtotalCents)
                    .discountAmount(discountCents)
                    .shippingAmount(shippingCents)
                    .taxAmount(taxCents)
                    .totalAmount(totalCents)
                    .currencyCode(currency)
                    .shippingAddressSnapshot(shippingAddressJson)
                    .billingAddressSnapshot(billingAddressJson)
                    .placedAt(Instant.now())
                    .build();

            for (CartItemEntity cartItem : cart.getItems()) {
                BookFormatEntity format = cartItem.getFormat();
                long itemSubtotal = format.getBasePriceAmount() * cartItem.getQuantity();

                long itemDiscount = (subtotalCents > 0) ? (discountCents * itemSubtotal) / subtotalCents : 0L;
                long itemTax = (subtotalCents > 0) ? (taxCents * itemSubtotal) / subtotalCents : 0L;

                OrderLineItemEntity lineItem = OrderLineItemEntity.builder()
                        .order(order)
                        .format(format)
                        .bookTitleSnapshot(format.getBook().getTitle())
                        .isbn13Snapshot(format.getBook().getIsbn13())
                        .formatTypeSnapshot(format.getFormatType())
                        .unitPriceAmount(format.getBasePriceAmount())
                        .quantity(cartItem.getQuantity())
                        .lineDiscountAmount(itemDiscount)
                        .lineTaxAmount(itemTax)
                        .lineTotalAmount(itemSubtotal)
                        .build();

                order.addLineItem(lineItem);
            }

            order.addStatusTransition("DRAFT", "PENDING_PAYMENT", "Order initialized. Awaiting payment capture.");
            OrderEntity saved = orderRepository.save(order);
            log.info("Order {} persisted in PENDING_PAYMENT status. Total: {} cents", orderNumber, totalCents);
            return saved;
        });

        // Phase 2: External payment capture executed OUTSIDE the database transaction
        try {
            paymentService.processOrderPayment(pendingOrder, request.getPaymentMethodToken(), "chk_" + pendingOrder.getOrderNumber());
        } catch (Exception paymentEx) {
            log.error("Payment failed for order {}. Initiating compensating inventory restock in dedicated transaction.",
                    pendingOrder.getOrderNumber(), paymentEx);

            transactionTemplate.executeWithoutResult(status -> {
                OrderEntity orderToFail = orderRepository.findById(pendingOrder.getId()).orElse(pendingOrder);
                for (OrderLineItemEntity lineItem : orderToFail.getLineItems()) {
                    bookFormatRepository.incrementInventory(lineItem.getFormat().getId(), lineItem.getQuantity());
                }
                orderToFail.addStatusTransition("PENDING_PAYMENT", "FAILED", "Payment processing failed: " + paymentEx.getMessage());
                orderRepository.save(orderToFail);
            });

            throw new BusinessRuleViolationException("Payment processing failed: " + paymentEx.getMessage());
        }

        // Phase 3: Finalize order confirmation and fulfillment in dedicated database transaction
        OrderEntity confirmedOrder = transactionTemplate.execute(status -> {
            OrderEntity orderToConfirm = orderRepository.findById(pendingOrder.getId()).orElse(pendingOrder);
            orderToConfirm.addStatusTransition("PENDING_PAYMENT", "CONFIRMED", "Payment authorized and captured. Order confirmed.");
            orderToConfirm.setConfirmedAt(Instant.now());

            if (orderToConfirm.getCoupon() != null) {
                try {
                    couponService.redeemCoupon(orderToConfirm.getCoupon().getId());
                } catch (Exception e) {
                    log.warn("Failed to increment coupon redemption counter: {}", e.getMessage());
                }
            }

            shippingService.createConsignment(orderToConfirm, "FEDEX", "Standard Ground", orderToConfirm.getShippingAmount());

            CartEntity cart = cartService.getCartEntityForCheckout(userId);
            cartService.clearCart(cart.getId());

            OrderEntity saved = orderRepository.save(orderToConfirm);
            log.info("Order {} successfully confirmed!", saved.getOrderNumber());
            return saved;
        });

        return orderMapper.toOrderConfirmationResponse(confirmedOrder);
    }

    /**
     * Retrieves comprehensive order invoice, line item snapshots, and delivery timeline.
     */
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetails(UUID userId, String orderNumber) {
        log.info("Fetching order details for order: {}, user: {}", orderNumber, userId);

        OrderEntity order = orderRepository.findByOrderNumberWithDetails(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        if (userId != null && order.getUser() != null && !order.getUser().getId().equals(userId)) {
            throw new BusinessRuleViolationException("You are not authorized to view this order.");
        }

        return orderMapper.toOrderDetailResponse(order);
    }

    /**
     * Lists paginated orders placed by customer with optional status filtering.
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> listOrders(UUID userId, String status, Pageable pageable) {
        log.info("Listing orders for user: {}, status: {}, page: {}", userId, status, pageable.getPageNumber());

        Page<OrderEntity> orders;
        if (status != null && !status.isBlank()) {
            orders = orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(userId, status.trim().toUpperCase(), pageable);
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return orders.map(orderMapper::toOrderSummaryDto);
    }

    /**
     * Cancels an order within self-service cancellation window, restocking inventory and issuing full refund.
     */
    @Transactional
    public OrderCancellationResponse cancelOrder(UUID userId, String orderNumber, CancelOrderRequest request) {
        log.info("Attempting cancellation for order {} by user {}", orderNumber, userId);

        OrderEntity order = orderRepository.findByOrderNumberWithDetails(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        if (userId != null && order.getUser() != null && !order.getUser().getId().equals(userId)) {
            throw new BusinessRuleViolationException("You are not authorized to cancel this order.");
        }

        if (!"PENDING_PAYMENT".equalsIgnoreCase(order.getOrderStatus()) && !"CONFIRMED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BusinessRuleViolationException(
                    "Order in status " + order.getOrderStatus() + " cannot be cancelled. Only unfulfilled orders are cancellable.");
        }

        // Restock inventory
        for (OrderLineItemEntity lineItem : order.getLineItems()) {
            bookFormatRepository.incrementInventory(lineItem.getFormat().getId(), lineItem.getQuantity());
            log.info("Restocked {} units of format SKU {}", lineItem.getQuantity(), lineItem.getFormat().getSku());
        }

        // Process refund
        paymentService.processRefund(order.getId(), order.getTotalAmount(), request.getReason());

        String previousStatus = order.getOrderStatus();
        order.addStatusTransition(previousStatus, "CANCELLED", "Customer self-service cancellation: " + request.getReason());
        order.setCancelledAt(Instant.now());
        order.setCancellationReason(request.getReason());

        OrderEntity cancelled = orderRepository.save(order);
        log.info("Order {} successfully cancelled.", orderNumber);

        return orderMapper.toOrderCancellationResponse(cancelled);
    }

    /**
     * Submits a customer Return Merchandise Authorization (RMA) request for delivered orders.
     */
    @Transactional
    public ReturnRequestEntity requestReturn(UUID userId, String orderNumber, String reasonCode, String reasonNotes) {
        log.info("Submitting return request for order: {} by user: {}", orderNumber, userId);

        OrderEntity order = orderRepository.findByOrderNumberWithDetails(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        if (userId != null && order.getUser() != null && !order.getUser().getId().equals(userId)) {
            throw new BusinessRuleViolationException("You are not authorized to request return for this order.");
        }

        if (!"DELIVERED".equalsIgnoreCase(order.getOrderStatus()) && !"CONFIRMED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new BusinessRuleViolationException("Returns can only be requested for confirmed or delivered orders.");
        }

        String rmaNumber = "RMA-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        ReturnRequestEntity rma = ReturnRequestEntity.builder()
                .order(order)
                .rmaNumber(rmaNumber)
                .returnStatus("REQUESTED")
                .reasonCode(reasonCode)
                .reasonNotes(reasonNotes)
                .refundAmount(order.getTotalAmount())
                .build();

        for (OrderLineItemEntity lineItem : order.getLineItems()) {
            ReturnRequestItemEntity rmaItem = ReturnRequestItemEntity.builder()
                    .returnRequest(rma)
                    .orderLineItem(lineItem)
                    .quantity(lineItem.getQuantity())
                    .itemCondition("UNOPENED")
                    .build();
            rma.addReturnItem(rmaItem);
        }

        ReturnRequestEntity saved = returnRequestRepository.save(rma);
        log.info("RMA request created with number: {}", rmaNumber);
        return saved;
    }

    // --- Private Helper Methods ---

    private String resolveShippingAddressJson(UUID userId, CheckoutOrderRequest request) {
        if (request.getShippingAddressId() != null) {
            UserAddressEntity address = userAddressRepository.findByIdAndUserId(request.getShippingAddressId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found: " + request.getShippingAddressId()));
            AddressDto dto = AddressDto.builder()
                    .recipientName(address.getRecipientName())
                    .streetLine1(address.getStreetLine1())
                    .streetLine2(address.getStreetLine2())
                    .city(address.getCity())
                    .stateOrProvince(address.getStateOrProvince())
                    .postalCode(address.getPostalCode())
                    .countryCode(address.getCountryCode())
                    .phoneNumber(address.getPhoneNumber())
                    .build();
            return serializeToJson(dto);
        } else if (request.getNewShippingAddress() != null) {
            return serializeToJson(request.getNewShippingAddress());
        }
        throw new BusinessRuleViolationException("Shipping address is required for checkout.");
    }

    private String resolveBillingAddressJson(UUID userId, CheckoutOrderRequest request, String defaultShippingJson) {
        if (request.getBillingAddressId() != null) {
            UserAddressEntity address = userAddressRepository.findByIdAndUserId(request.getBillingAddressId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Billing address not found: " + request.getBillingAddressId()));
            AddressDto dto = AddressDto.builder()
                    .recipientName(address.getRecipientName())
                    .streetLine1(address.getStreetLine1())
                    .streetLine2(address.getStreetLine2())
                    .city(address.getCity())
                    .stateOrProvince(address.getStateOrProvince())
                    .postalCode(address.getPostalCode())
                    .countryCode(address.getCountryCode())
                    .phoneNumber(address.getPhoneNumber())
                    .build();
            return serializeToJson(dto);
        } else if (request.getNewBillingAddress() != null) {
            return serializeToJson(request.getNewBillingAddress());
        }
        return defaultShippingJson;
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new BusinessRuleViolationException("Failed to serialize address snapshot: " + e.getMessage());
        }
    }
}
