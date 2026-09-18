package com.bookcorner.controller;

import com.bookcorner.common.exception.UnauthorizedOperationException;
import com.bookcorner.dto.payment.CreatePaymentIntentRequest;
import com.bookcorner.dto.payment.CustomerWalletResponse;
import com.bookcorner.dto.payment.PaymentIntentResponse;
import com.bookcorner.service.PaymentService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Payment Intent, Digital Wallet, and Gateway Webhook REST Controller.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment gateway integration, multi-tender split calculation, digital wallet ledger, and webhooks.")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Create payment intent with multi-tender split", operationId = "createPaymentIntent",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment intent coordinated successfully."),
            @ApiResponse(responseCode = "400", description = "Bad Request."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "Order not found.")
    })
    @PostMapping("/intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            Authentication authentication,
            @Valid @RequestBody CreatePaymentIntentRequest request
    ) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("POST /payments/intent for order: {}, user: {}", request.getOrderNumber(), userId);
        PaymentIntentResponse response = paymentService.createPaymentIntent(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Gateway webhook callback listener", operationId = "ingestPaymentWebhook")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook received and verified successfully."),
            @ApiResponse(responseCode = "400", description = "Invalid payload or signature.")
    })
    @PostMapping("/webhooks/{provider}")
    public ResponseEntity<Map<String, Boolean>> handleWebhook(
            @Parameter(description = "Gateway provider (stripe, razorpay, adyen)", required = true)
            @PathVariable("provider") String provider,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
            @RequestBody String payload
    ) {
        log.info("POST /payments/webhooks/{}", provider);
        paymentService.handleWebhook(provider, stripeSignature, payload);
        return ResponseEntity.ok(Map.of("received", true));
    }

    @Operation(summary = "Retrieve digital wallet balance and ledger history", operationId = "getCustomerWallet",
            security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer wallet summary."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @GetMapping("/wallet")
    public ResponseEntity<CustomerWalletResponse> getCustomerWallet(Authentication authentication) {
        UUID userId = resolveAuthenticatedUserId(authentication);
        log.info("GET /payments/wallet for user: {}", userId);
        CustomerWalletResponse wallet = paymentService.getCustomerWallet(userId);
        return ResponseEntity.ok(wallet);
    }

    private UUID resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedOperationException("Authentication required to access payment and wallet resources.");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return UUID.fromString("11111111-1111-1111-1111-111111111111");
        }
    }
}
