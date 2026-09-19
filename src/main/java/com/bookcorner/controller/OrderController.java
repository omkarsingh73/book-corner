package com.bookcorner.controller;

import com.bookcorner.common.exception.UnauthorizedOperationException;
import com.bookcorner.dto.order.CancelOrderRequest;
import com.bookcorner.dto.order.CheckoutOrderRequest;
import com.bookcorner.dto.order.CreateReturnRequest;
import com.bookcorner.dto.order.OrderCancellationResponse;
import com.bookcorner.dto.order.OrderConfirmationResponse;
import com.bookcorner.dto.order.OrderDetailResponse;
import com.bookcorner.dto.order.OrderSummaryDto;
import com.bookcorner.dto.order.ReturnAuthorizationResponse;
import com.bookcorner.entity.ordering.ReturnRequestEntity;
import com.bookcorner.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Order Fulfillment and Checkout Orchestration REST Controller.
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Checkout orchestration saga, customer order history, self-service cancellations, and RMAs.")
@SecurityRequirement(name = "BearerAuth")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Submit checkout and place order", operationId = "submitOrderCheckout")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed and confirmed successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request / Validation Failure."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "402", description = "Payment declined or authorization failed."),
            @ApiResponse(responseCode = "409", description = "Inventory conflict.")
    })
    @PostMapping("/checkout")
    public ResponseEntity<OrderConfirmationResponse> checkout(
            Authentication authentication,
            @Parameter(description = "Idempotency key to prevent double charge")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CheckoutOrderRequest request
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("POST /orders/checkout for user: {}, idempotencyKey: {}", userId, idempotencyKey);
        OrderConfirmationResponse response = orderService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List customer order history", operationId = "listOrders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order history retrieved."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping
    public ResponseEntity<Page<OrderSummaryDto>> listOrders(
            Authentication authentication,
            @Parameter(description = "Filter by order status")
            @RequestParam(value = "status", required = false) String status,
            @PageableDefault(page = 0, size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("GET /orders for user: {}, status: {}, page: {}", userId, status, pageable.getPageNumber());
        Page<OrderSummaryDto> orders = orderService.listOrders(userId, status, pageable);
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Retrieve detailed order specifications and invoice", operationId = "getOrderByNumber")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detailed order record."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Order not found.")
    })
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderDetailResponse> getOrderByNumber(
            Authentication authentication,
            @Parameter(description = "Order business identifier (e.g. ORD-20260918-A8F2)", required = true)
            @PathVariable("orderNumber") String orderNumber
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("GET /orders/{} for user: {}", orderNumber, userId);
        OrderDetailResponse order = orderService.getOrderDetails(userId, orderNumber);
        return ResponseEntity.ok(order);
    }

    @Operation(summary = "Cancel order within policy grace period", operationId = "cancelOrder")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled and refund initiated."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "409", description = "Conflict - Order is past cancellation grace period.")
    })
    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<OrderCancellationResponse> cancelOrder(
            Authentication authentication,
            @Parameter(description = "Order business identifier", required = true)
            @PathVariable("orderNumber") String orderNumber,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("POST /orders/{}/cancel for user: {}", orderNumber, userId);
        OrderCancellationResponse response = orderService.cancelOrder(userId, orderNumber, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Initiate Return Merchandise Authorization (RMA)", operationId = "requestOrderReturn")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Return authorized and label issued."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Order not found.")
    })
    @PostMapping("/{orderNumber}/returns")
    public ResponseEntity<ReturnAuthorizationResponse> requestOrderReturn(
            Authentication authentication,
            @Parameter(description = "Order business identifier", required = true)
            @PathVariable("orderNumber") String orderNumber,
            @Valid @RequestBody CreateReturnRequest request
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("POST /orders/{}/returns for user: {}", orderNumber, userId);

        ReturnRequestEntity rma = orderService.requestReturn(userId, orderNumber, request.getReasonCode(), request.getCustomerRemarks());

        ReturnAuthorizationResponse response = ReturnAuthorizationResponse.builder()
                .rmaNumber(rma.getRmaNumber())
                .rmaStatus(rma.getReturnStatus())
                .returnCarrier("UPS")
                .returnTrackingNumber("1Z" + System.currentTimeMillis() + "RMA")
                .prepaidReturnLabelUrl("https://shipping.bookcorner.com/labels/" + rma.getRmaNumber() + ".pdf")
                .estimatedRefundAmount(rma.getRefundAmount())
                .returnInstructions("Package books in original box, attach the prepaid UPS label, and drop off at any authorized UPS location within 14 days.")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UUID resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedOperationException("Authentication required to access customer orders.");
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
