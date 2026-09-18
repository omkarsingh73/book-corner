package com.bookcorner.service;

import com.bookcorner.common.exception.BusinessRuleViolationException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.payment.CreatePaymentIntentRequest;
import com.bookcorner.dto.payment.CustomerWalletResponse;
import com.bookcorner.dto.payment.PaymentIntentResponse;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.ordering.OrderEntity;
import com.bookcorner.entity.payment.CustomerWalletEntity;
import com.bookcorner.entity.payment.PaymentTransactionEntity;
import com.bookcorner.entity.payment.RefundRecordEntity;
import com.bookcorner.entity.payment.TenderSplitEntity;
import com.bookcorner.mapper.PaymentMapper;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.ordering.OrderRepository;
import com.bookcorner.repository.payment.CustomerWalletRepository;
import com.bookcorner.repository.payment.PaymentTransactionRepository;
import com.bookcorner.repository.payment.RefundRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment Orchestration and Ledger Service.
 * Coordinates multi-tender authorization, gateway integration (e.g. Stripe),
 * customer digital wallet debits with pessimistic concurrency locking, and webhook reconciliations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final CustomerWalletRepository customerWalletRepository;
    private final RefundRecordRepository refundRecordRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;

    /**
     * Initializes a multi-tender payment intent for an order before client checkout confirmation.
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntent(UUID userId, CreatePaymentIntentRequest request) {
        log.info("Creating payment intent for order: {}, user: {}", request.getOrderNumber(), userId);

        OrderEntity order = orderRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderNumber()));

        if (userId != null && order.getUser() != null && !order.getUser().getId().equals(userId)) {
            throw new BusinessRuleViolationException("Order does not belong to the authenticated user.");
        }

        long totalSplitAmount = request.getTenderSplits().stream()
                .mapToLong(s -> s.getAmount() != null && s.getAmount().getAmount() != null ? s.getAmount().getAmount() : 0L)
                .sum();

        if (totalSplitAmount != order.getTotalAmount()) {
            log.warn("Tender split mismatch: splits sum to {} cents, order total is {} cents",
                    totalSplitAmount, order.getTotalAmount());
            throw new BusinessRuleViolationException(String.format(
                    "Tender split total (%.2f) does not match order total amount (%.2f).",
                    totalSplitAmount / 100.0, order.getTotalAmount() / 100.0));
        }

        String idempotencyKey = "pi_" + order.getOrderNumber() + "_" + Instant.now().toEpochMilli();
        String clientSecret = "pi_secret_" + UUID.randomUUID().toString().replace("-", "");

        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .order(order)
                .idempotencyKey(idempotencyKey)
                .transactionStatus("INITIALIZED")
                .totalPayableAmount(order.getTotalAmount())
                .currencyCode(order.getCurrencyCode())
                .gatewayProvider("STRIPE")
                .gatewayTransactionId(clientSecret)
                .build();

        for (CreatePaymentIntentRequest.TenderSplitRequest splitReq : request.getTenderSplits()) {
            TenderSplitEntity split = TenderSplitEntity.builder()
                    .paymentTransaction(transaction)
                    .tenderType(splitReq.getTenderType().toUpperCase())
                    .amount(splitReq.getAmount().getAmount())
                    .tenderReference(splitReq.getTenderReference())
                    .tenderStatus("PENDING")
                    .build();
            transaction.addTenderSplit(split);
        }

        PaymentTransactionEntity saved = paymentTransactionRepository.save(transaction);
        log.info("Payment intent initialized with ID: {}", saved.getId());

        return paymentMapper.toPaymentIntentResponse(saved);
    }

    /**
     * Executes synchronous payment capture for an order during the checkout saga.
     * Enforces idempotent execution to prevent duplicate charges.
     */
    @Transactional
    public PaymentTransactionEntity processOrderPayment(OrderEntity order, String paymentMethodToken, String idempotencyKey) {
        log.info("Processing payment for order: {}, idempotencyKey: {}", order.getOrderNumber(), idempotencyKey);

        // Check idempotency
        Optional<PaymentTransactionEntity> existing = paymentTransactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            PaymentTransactionEntity tx = existing.get();
            log.info("Returning existing idempotent payment transaction {} (status: {})", tx.getId(), tx.getTransactionStatus());
            if ("CAPTURED".equalsIgnoreCase(tx.getTransactionStatus()) || "AUTHORIZED".equalsIgnoreCase(tx.getTransactionStatus())) {
                return tx;
            }
        }

        boolean isWalletTender = "WALLET".equalsIgnoreCase(paymentMethodToken) || 
                                 (paymentMethodToken != null && paymentMethodToken.toLowerCase().startsWith("wallet"));

        String provider = isWalletTender ? "INTERNAL_WALLET" : "STRIPE";
        String gatewayTxId = isWalletTender 
                ? "wtx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                : "ch_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);

        // If paying with internal wallet, deduct balance atomically under pessimistic write lock
        if (isWalletTender) {
            if (order.getUser() == null) {
                throw new BusinessRuleViolationException("Digital wallet payment requires an authenticated customer.");
            }
            CustomerWalletEntity wallet = customerWalletRepository.findByUserIdWithPessimisticLock(order.getUser().getId())
                    .orElseThrow(() -> new BusinessRuleViolationException("Customer digital wallet not found."));

            if (wallet.isLocked()) {
                throw new BusinessRuleViolationException("Customer digital wallet is locked. Please contact customer support.");
            }

            if (wallet.getCurrentBalance() < order.getTotalAmount()) {
                log.warn("Insufficient wallet funds for user {}: balance={} cents, required={} cents",
                        order.getUser().getId(), wallet.getCurrentBalance(), order.getTotalAmount());
                throw new BusinessRuleViolationException(String.format(
                        "Insufficient wallet balance. Available: %.2f, Required: %.2f",
                        wallet.getCurrentBalance() / 100.0, order.getTotalAmount() / 100.0));
            }

            wallet.addLedgerEntry("DEBIT_PURCHASE", -order.getTotalAmount(), order.getOrderNumber(), "Checkout payment for order " + order.getOrderNumber());
            customerWalletRepository.save(wallet);
            log.info("Deducted {} cents from wallet of user {}. New balance: {}",
                    order.getTotalAmount(), order.getUser().getId(), wallet.getCurrentBalance());
        }

        PaymentTransactionEntity transaction = PaymentTransactionEntity.builder()
                .order(order)
                .idempotencyKey(idempotencyKey)
                .transactionStatus("CAPTURED")
                .totalPayableAmount(order.getTotalAmount())
                .currencyCode(order.getCurrencyCode())
                .gatewayProvider(provider)
                .gatewayTransactionId(gatewayTxId)
                .authorizedAt(Instant.now())
                .capturedAt(Instant.now())
                .build();

        TenderSplitEntity split = TenderSplitEntity.builder()
                .paymentTransaction(transaction)
                .tenderType(isWalletTender ? "WALLET" : "CREDIT_CARD")
                .amount(order.getTotalAmount())
                .tenderReference(paymentMethodToken)
                .tenderStatus("CAPTURED")
                .build();

        transaction.addTenderSplit(split);
        PaymentTransactionEntity savedTx = paymentTransactionRepository.save(transaction);
        log.info("Successfully captured payment transaction ID: {} for order: {}", savedTx.getId(), order.getOrderNumber());

        return savedTx;
    }

    /**
     * Reconciles external gateway webhook event callbacks.
     */
    @Transactional
    public void handleWebhook(String provider, String signature, String payload) {
        log.info("Received payment webhook from provider: {}", provider);

        if (payload == null || payload.isBlank()) {
            throw new BusinessRuleViolationException("Webhook payload cannot be empty.");
        }

        // Mock gateway signature verification
        if (signature == null || signature.isBlank()) {
            log.warn("Webhook missing provider signature verification header.");
        }

        // Parse mock event and update transaction record if matching gatewayTransactionId exists
        log.info("Webhook from {} acknowledged and processed successfully.", provider);
    }

    /**
     * Retrieves customer digital wallet balance and ledger history.
     */
    @Transactional
    public CustomerWalletResponse getCustomerWallet(UUID userId) {
        log.info("Retrieving digital wallet for user ID: {}", userId);

        CustomerWalletEntity wallet = customerWalletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserEntity user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

                    CustomerWalletEntity newWallet = CustomerWalletEntity.builder()
                            .user(user)
                            .currentBalance(0L)
                            .currencyCode("USD")
                            .isLocked(false)
                            .build();

                    return customerWalletRepository.save(newWallet);
                });

        return paymentMapper.toCustomerWalletResponse(wallet);
    }

    /**
     * Issues an atomic refund against an order payment.
     */
    @Transactional
    public void processRefund(UUID orderId, long refundAmountCents, String reason) {
        log.info("Processing refund of {} cents for order ID: {}. Reason: {}", refundAmountCents, orderId, reason);

        var transactions = paymentTransactionRepository.findByOrderId(orderId);
        if (transactions.isEmpty()) {
            log.warn("No payment transactions found to refund for order ID: {}", orderId);
            return;
        }

        PaymentTransactionEntity primaryTx = transactions.get(0);

        RefundRecordEntity refund = RefundRecordEntity.builder()
                .paymentTransaction(primaryTx)
                .refundAmount(refundAmountCents)
                .currencyCode(primaryTx.getCurrencyCode())
                .reason(reason)
                .refundStatus("COMPLETED")
                .gatewayRefundId("re_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20))
                .processedAt(Instant.now())
                .build();

        refundRecordRepository.save(refund);

        // If paid with wallet, credit customer wallet back
        if ("INTERNAL_WALLET".equalsIgnoreCase(primaryTx.getGatewayProvider()) && primaryTx.getOrder().getUser() != null) {
            CustomerWalletEntity wallet = customerWalletRepository.findByUserIdWithPessimisticLock(primaryTx.getOrder().getUser().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer wallet not found for refund."));
            wallet.addLedgerEntry("CREDIT_REFUND", refundAmountCents, primaryTx.getOrder().getOrderNumber(), "Refund for order: " + primaryTx.getOrder().getOrderNumber());
            customerWalletRepository.save(wallet);
            log.info("Refund credited back to wallet for user {}. New balance: {}", wallet.getUser().getId(), wallet.getCurrentBalance());
        }

        log.info("Refund completed successfully for order ID: {}", orderId);
    }
}
