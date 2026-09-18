package com.bookcorner.service;

import com.bookcorner.common.exception.BusinessRuleViolationException;
import com.bookcorner.common.exception.ResourceNotFoundException;
import com.bookcorner.dto.common.MoneyDto;
import com.bookcorner.dto.payment.CreatePaymentIntentRequest;
import com.bookcorner.dto.payment.CustomerWalletResponse;
import com.bookcorner.dto.payment.PaymentIntentResponse;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.ordering.OrderEntity;
import com.bookcorner.entity.payment.CustomerWalletEntity;
import com.bookcorner.entity.payment.PaymentTransactionEntity;
import com.bookcorner.entity.payment.RefundRecordEntity;
import com.bookcorner.mapper.PaymentMapper;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.ordering.OrderRepository;
import com.bookcorner.repository.payment.CustomerWalletRepository;
import com.bookcorner.repository.payment.PaymentTransactionRepository;
import com.bookcorner.repository.payment.RefundRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests (Mockito & AssertJ)")
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private CustomerWalletRepository customerWalletRepository;

    @Mock
    private RefundRecordRepository refundRecordRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    private UUID userId;
    private UserEntity user;
    private OrderEntity order;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = UserEntity.builder()
                .id(userId)
                .email("buyer@example.com")
                .firstName("Alice")
                .lastName("Smith")
                .accountStatus("ACTIVE")
                .build();

        order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-20260918-1001")
                .user(user)
                .totalAmount(5000L) // $50.00
                .currencyCode("USD")
                .orderStatus("PENDING_PAYMENT")
                .tenderSplits(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should create payment intent successfully when split total matches order amount")
    void shouldCreatePaymentIntentSuccessfully() {
        CreatePaymentIntentRequest request = CreatePaymentIntentRequest.builder()
                .orderNumber(order.getOrderNumber())
                .tenderSplits(List.of(
                        CreatePaymentIntentRequest.TenderSplitRequest.builder()
                                .tenderType("CREDIT_CARD")
                                .amount(MoneyDto.builder().amount(5000L).currency("USD").build())
                                .tenderReference("pm_card_test")
                                .build()
                ))
                .build();

        when(orderRepository.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentIntentResponse mockResponse = PaymentIntentResponse.builder()
                .idempotencyKey("pi_key")
                .transactionStatus("INITIALIZED")
                .build();
        when(paymentMapper.toPaymentIntentResponse(any(PaymentTransactionEntity.class))).thenReturn(mockResponse);

        PaymentIntentResponse response = paymentService.createPaymentIntent(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTransactionStatus()).isEqualTo("INITIALIZED");
        verify(paymentTransactionRepository).save(any(PaymentTransactionEntity.class));
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when tender split sum differs from order total")
    void shouldThrowExceptionWhenTenderSplitsMismatch() {
        CreatePaymentIntentRequest request = CreatePaymentIntentRequest.builder()
                .orderNumber(order.getOrderNumber())
                .tenderSplits(List.of(
                        CreatePaymentIntentRequest.TenderSplitRequest.builder()
                                .tenderType("CREDIT_CARD")
                                .amount(MoneyDto.builder().amount(4000L).currency("USD").build()) // Only $40
                                .build()
                ))
                .build();

        when(orderRepository.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.createPaymentIntent(userId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("does not match order total");

        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when order belongs to different user")
    void shouldThrowExceptionWhenOrderBelongsToDifferentUser() {
        UUID differentUserId = UUID.randomUUID();
        CreatePaymentIntentRequest request = CreatePaymentIntentRequest.builder()
                .orderNumber(order.getOrderNumber())
                .tenderSplits(List.of(
                        CreatePaymentIntentRequest.TenderSplitRequest.builder()
                                .tenderType("CREDIT_CARD")
                                .amount(MoneyDto.builder().amount(5000L).currency("USD").build())
                                .build()
                ))
                .build();

        when(orderRepository.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.createPaymentIntent(differentUserId, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Order does not belong to the authenticated user");
    }

    @Test
    @DisplayName("Should process credit card order payment successfully")
    void shouldProcessCreditCardPaymentSuccessfully() {
        when(paymentTransactionRepository.findByIdempotencyKey("chk_ORD-1")).thenReturn(Optional.empty());
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransactionEntity tx = paymentService.processOrderPayment(order, "pm_card_mastercard", "chk_ORD-1");

        assertThat(tx).isNotNull();
        assertThat(tx.getTransactionStatus()).isEqualTo("CAPTURED");
        assertThat(tx.getGatewayProvider()).isEqualTo("STRIPE");
        assertThat(tx.getTotalPayableAmount()).isEqualTo(5000L);
        assertThat(tx.getTenderSplits()).hasSize(1);
        assertThat(tx.getTenderSplits().get(0).getTenderType()).isEqualTo("CREDIT_CARD");
    }

    @Test
    @DisplayName("Should return existing payment transaction when idempotency key already exists and is captured")
    void shouldReturnExistingIdempotentTransaction() {
        PaymentTransactionEntity existingTx = PaymentTransactionEntity.builder()
                .id(UUID.randomUUID())
                .order(order)
                .idempotencyKey("chk_ORD-1")
                .transactionStatus("CAPTURED")
                .totalPayableAmount(5000L)
                .build();

        when(paymentTransactionRepository.findByIdempotencyKey("chk_ORD-1")).thenReturn(Optional.of(existingTx));

        PaymentTransactionEntity result = paymentService.processOrderPayment(order, "pm_card_mastercard", "chk_ORD-1");

        assertThat(result).isSameAs(existingTx);
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should process wallet payment with pessimistic lock and balance deduction")
    void shouldProcessWalletPaymentSuccessfully() {
        CustomerWalletEntity wallet = CustomerWalletEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .currentBalance(10000L) // $100.00
                .currencyCode("USD")
                .isLocked(false)
                .ledgerEntries(new ArrayList<>())
                .build();

        when(paymentTransactionRepository.findByIdempotencyKey("chk_WALLET_1")).thenReturn(Optional.empty());
        when(customerWalletRepository.findByUserIdWithPessimisticLock(userId)).thenReturn(Optional.of(wallet));
        when(paymentTransactionRepository.save(any(PaymentTransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentTransactionEntity tx = paymentService.processOrderPayment(order, "WALLET", "chk_WALLET_1");

        assertThat(tx).isNotNull();
        assertThat(tx.getGatewayProvider()).isEqualTo("INTERNAL_WALLET");
        assertThat(wallet.getCurrentBalance()).isEqualTo(5000L); // 10000 - 5000
        assertThat(wallet.getLedgerEntries()).hasSize(1);
        assertThat(wallet.getLedgerEntries().get(0).getEntryType()).isEqualTo("DEBIT_PURCHASE");

        verify(customerWalletRepository).save(wallet);
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when wallet balance is insufficient")
    void shouldThrowExceptionWhenWalletBalanceInsufficient() {
        CustomerWalletEntity wallet = CustomerWalletEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .currentBalance(2000L) // Only $20.00, order is $50.00
                .currencyCode("USD")
                .isLocked(false)
                .ledgerEntries(new ArrayList<>())
                .build();

        when(paymentTransactionRepository.findByIdempotencyKey("chk_WALLET_2")).thenReturn(Optional.empty());
        when(customerWalletRepository.findByUserIdWithPessimisticLock(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> paymentService.processOrderPayment(order, "WALLET", "chk_WALLET_2"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Insufficient wallet balance");

        verify(customerWalletRepository, never()).save(any());
        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BusinessRuleViolationException when wallet is locked")
    void shouldThrowExceptionWhenWalletIsLocked() {
        CustomerWalletEntity wallet = CustomerWalletEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .currentBalance(10000L)
                .currencyCode("USD")
                .isLocked(true) // Locked
                .build();

        when(paymentTransactionRepository.findByIdempotencyKey("chk_WALLET_3")).thenReturn(Optional.empty());
        when(customerWalletRepository.findByUserIdWithPessimisticLock(userId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> paymentService.processOrderPayment(order, "WALLET", "chk_WALLET_3"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("wallet is locked");
    }

    @Test
    @DisplayName("Should retrieve existing customer wallet")
    void shouldGetCustomerWalletSuccessfully() {
        CustomerWalletEntity wallet = CustomerWalletEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .currentBalance(7500L)
                .currencyCode("USD")
                .build();

        when(customerWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        CustomerWalletResponse responseDto = CustomerWalletResponse.builder()
                .currencyCode("USD")
                .currentBalance(MoneyDto.builder().amount(7500L).currency("USD").build())
                .build();
        when(paymentMapper.toCustomerWalletResponse(wallet)).thenReturn(responseDto);

        CustomerWalletResponse result = paymentService.getCustomerWallet(userId);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentBalance().getAmount()).isEqualTo(7500L);
    }

    @Test
    @DisplayName("Should create brand new zero-balance wallet when user has none")
    void shouldCreateNewWalletWhenNoneExists() {
        when(customerWalletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerWalletRepository.save(any(CustomerWalletEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerWalletResponse responseDto = CustomerWalletResponse.builder()
                .currencyCode("USD")
                .currentBalance(MoneyDto.builder().amount(0L).currency("USD").build())
                .build();
        when(paymentMapper.toCustomerWalletResponse(any(CustomerWalletEntity.class))).thenReturn(responseDto);

        CustomerWalletResponse result = paymentService.getCustomerWallet(userId);

        assertThat(result).isNotNull();
        assertThat(result.getCurrentBalance().getAmount()).isEqualTo(0L);
        verify(customerWalletRepository).save(any(CustomerWalletEntity.class));
    }

    @Test
    @DisplayName("Should process refund and credit wallet back for internal wallet payment")
    void shouldProcessRefundForWalletPayment() {
        UUID orderId = order.getId();
        PaymentTransactionEntity tx = PaymentTransactionEntity.builder()
                .id(UUID.randomUUID())
                .order(order)
                .gatewayProvider("INTERNAL_WALLET")
                .currencyCode("USD")
                .totalPayableAmount(5000L)
                .build();

        CustomerWalletEntity wallet = CustomerWalletEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .currentBalance(2000L)
                .currencyCode("USD")
                .ledgerEntries(new ArrayList<>())
                .build();

        when(paymentTransactionRepository.findByOrderId(orderId)).thenReturn(List.of(tx));
        when(customerWalletRepository.findByUserIdWithPessimisticLock(userId)).thenReturn(Optional.of(wallet));
        when(refundRecordRepository.save(any(RefundRecordEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentService.processRefund(orderId, 5000L, "Customer cancellation");

        verify(refundRecordRepository).save(any(RefundRecordEntity.class));
        assertThat(wallet.getCurrentBalance()).isEqualTo(7000L); // 2000 + 5000
        assertThat(wallet.getLedgerEntries()).hasSize(1);
        assertThat(wallet.getLedgerEntries().get(0).getEntryType()).isEqualTo("CREDIT_REFUND");
        verify(customerWalletRepository).save(wallet);
    }
}
