package com.bookcorner.integration;

import com.bookcorner.dto.auth.AuthTokenResponse;
import com.bookcorner.dto.auth.RegisterRequest;
import com.bookcorner.dto.cart.AddCartItemRequest;
import com.bookcorner.dto.cart.ApplyCouponRequest;
import com.bookcorner.dto.common.AddressDto;
import com.bookcorner.dto.order.CancelOrderRequest;
import com.bookcorner.dto.order.CheckoutOrderRequest;
import com.bookcorner.dto.order.OrderCancellationResponse;
import com.bookcorner.dto.order.OrderConfirmationResponse;
import com.bookcorner.dto.order.OrderDetailResponse;
import com.bookcorner.entity.catalog.AuthorEntity;
import com.bookcorner.entity.catalog.BookEntity;
import com.bookcorner.entity.catalog.BookFormatEntity;
import com.bookcorner.entity.catalog.CategoryEntity;
import com.bookcorner.entity.catalog.PublisherEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.ordering.CartEntity;
import com.bookcorner.entity.ordering.CouponEntity;
import com.bookcorner.entity.ordering.OrderEntity;
import com.bookcorner.entity.payment.PaymentTransactionEntity;
import com.bookcorner.entity.payment.RefundRecordEntity;
import com.bookcorner.entity.shipping.ShippingConsignmentEntity;
import com.bookcorner.entity.store.StoreEntity;
import com.bookcorner.repository.catalog.AuthorRepository;
import com.bookcorner.repository.catalog.BookFormatRepository;
import com.bookcorner.repository.catalog.BookRepository;
import com.bookcorner.repository.catalog.CategoryRepository;
import com.bookcorner.repository.catalog.PublisherRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.ordering.CartRepository;
import com.bookcorner.repository.ordering.CouponRepository;
import com.bookcorner.repository.ordering.OrderRepository;
import com.bookcorner.repository.payment.PaymentTransactionRepository;
import com.bookcorner.repository.payment.RefundRecordRepository;
import com.bookcorner.repository.shipping.ShippingConsignmentRepository;
import com.bookcorner.repository.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("End-to-End Order Workflow Saga Integration Tests (PostgreSQL Testcontainer)")
class OrderWorkflowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookFormatRepository bookFormatRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private RefundRecordRepository refundRecordRepository;

    @Autowired
    private ShippingConsignmentRepository shippingConsignmentRepository;

    private StoreEntity store;
    private PublisherEntity publisher;
    private CategoryEntity category;
    private AuthorEntity author;
    private BookEntity book;
    private BookFormatEntity format;
    private CouponEntity coupon;
    private UserEntity customer;
    private String customerJwt;
    private UserEntity otherCustomer;
    private String otherCustomerJwt;

    @BeforeEach
    void setUpOrderWorkflowEnvironment() throws Exception {
        // 1. Store
        store = storeRepository.findByStoreCode("BK_STORE_SAGA")
                .orElseGet(() -> storeRepository.save(StoreEntity.builder()
                        .storeCode("BK_STORE_SAGA")
                        .storeName("Book Corner Saga Store")
                        .defaultCurrency("USD")
                        .isActive(true)
                        .build()));

        // 2. Publisher
        publisher = publisherRepository.findByPublisherCode("PUB-OREILLY-SAGA")
                .orElseGet(() -> publisherRepository.save(PublisherEntity.builder()
                        .publisherCode("PUB-OREILLY-SAGA")
                        .publisherName("O'Reilly Media")
                        .contactEmail("contact@oreilly.com")
                        .build()));

        // 3. Category
        category = categoryRepository.findBySlug("distributed-systems-saga")
                .orElseGet(() -> categoryRepository.save(CategoryEntity.builder()
                        .categoryName("Distributed Systems")
                        .categorySlug("distributed-systems-saga")
                        .displayOrder(1)
                        .build()));

        // 4. Author
        author = authorRepository.findByAuthorSlug("martin-kleppmann-saga")
                .orElseGet(() -> authorRepository.save(AuthorEntity.builder()
                        .fullName("Martin Kleppmann")
                        .authorSlug("martin-kleppmann-saga")
                        .biography("Researcher in distributed systems at the University of Cambridge")
                        .build()));

        // 5. Book & Format
        String isbn = "9781449373320";
        if (bookRepository.findByIsbn13(isbn).isEmpty()) {
            BookEntity newBook = BookEntity.builder()
                    .isbn13(isbn)
                    .title("Designing Data-Intensive Applications")
                    .subtitle("The Big Ideas Behind Reliable, Scalable, and Maintainable Systems")
                    .synopsis("Comprehensive guide to distributed storage and processing.")
                    .publisher(publisher)
                    .primaryCategory(category)
                    .publicationDate(LocalDate.of(2017, 3, 16))
                    .primaryLanguage("English")
                    .pageCount(616)
                    .build();

            newBook.addAuthor(author, 1, "AUTHOR");

            BookFormatEntity newFormat = BookFormatEntity.builder()
                    .book(newBook)
                    .sku("SKU-DDIA-PB")
                    .formatType("PAPERBACK")
                    .basePriceAmount(4500L) // $45.00
                    .currencyCode("USD")
                    .stockQuantity(10) // 10 copies in stock
                    .build();
            newBook.addFormat(newFormat);

            book = bookRepository.save(newBook);
            format = book.getFormats().get(0);
        } else {
            book = bookRepository.findByIsbn13(isbn).get();
            format = book.getFormats().get(0);
            // Reset inventory to 10 for deterministic testing
            format.setInventoryQuantity(10);
            bookFormatRepository.save(format);
        }

        // 6. Promotional Coupon (20% off, max $20.00, min order $30.00)
        String couponCode = "SAGA20";
        coupon = couponRepository.findByCouponCode(couponCode)
                .orElseGet(() -> couponRepository.save(CouponEntity.builder()
                        .couponCode(couponCode)
                        .discountType("PERCENTAGE")
                        .discountValue(20L)
                        .minimumOrderAmount(3000L) // $30.00
                        .maxDiscountAmount(2000L) // $20.00
                        .currentRedemptions(0)
                        .maxRedemptions(50)
                        .validFrom(Instant.now().minus(1, ChronoUnit.DAYS))
                        .validUntil(Instant.now().plus(30, ChronoUnit.DAYS))
                        .isActive(true)
                        .build()));
        coupon.setCurrentRedemptions(0);
        couponRepository.save(coupon);

        // 7. Register Primary Customer
        String customerEmail = "saga_buyer_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        RegisterRequest registerReq = RegisterRequest.builder()
                .email(customerEmail)
                .password("Password123!")
                .firstName("Saga")
                .lastName("Buyer")
                .build();

        MvcResult regResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthTokenResponse authTokens = objectMapper.readValue(
                regResult.getResponse().getContentAsString(), AuthTokenResponse.class);
        customerJwt = authTokens.getAccessToken();
        customer = userRepository.findByEmail(customerEmail).orElseThrow();

        // 8. Register Other Customer for security isolation verification
        String otherEmail = "other_buyer_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        RegisterRequest otherReq = RegisterRequest.builder()
                .email(otherEmail)
                .password("Password123!")
                .firstName("Other")
                .lastName("User")
                .build();

        MvcResult otherResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthTokenResponse otherTokens = objectMapper.readValue(
                otherResult.getResponse().getContentAsString(), AuthTokenResponse.class);
        otherCustomerJwt = otherTokens.getAccessToken();
        otherCustomer = userRepository.findByEmail(otherEmail).orElseThrow();
    }

    @Test
    @DisplayName("Complete Order Workflow Saga: AddToCart -> ApplyCoupon -> Checkout -> InvariantVerification -> Query -> CancelRestock")
    void shouldExecuteCompleteEndToEndOrderCheckoutSagaAndCancelWithInventoryRestock() throws Exception {
        // =========================================================================
        // STEP 1: Add format item (2 units) to customer cart
        // =========================================================================
        AddCartItemRequest addItemReq = AddCartItemRequest.builder()
                .formatId(format.getId())
                .quantity(2)
                .build();

        mockMvc.perform(post("/cart/items")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addItemReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].format.id").value(format.getId().toString()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.subtotal.amount").value(9000)); // $45.00 * 2 = $90.00

        // =========================================================================
        // STEP 2: Apply promotional coupon 'SAGA20' to cart
        // =========================================================================
        ApplyCouponRequest couponReq = ApplyCouponRequest.builder()
                .couponCode("SAGA20")
                .build();

        mockMvc.perform(post("/cart/coupon")
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(couponReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCoupon.couponCode").value("SAGA20"))
                .andExpect(jsonPath("$.discount.amount").value(1800)); // 20% of 9000 = 1800 ($18.00)

        // =========================================================================
        // STEP 3: Execute Checkout Saga
        // =========================================================================
        AddressDto shippingAddress = AddressDto.builder()
                .recipientName("Martin Kleppmann")
                .streetLine1("10 Downing Street")
                .city("London")
                .stateOrProvince("Greater London")
                .postalCode("SW1A 2AA")
                .countryCode("GBR")
                .phoneNumber("+442079460919")
                .build();

        CheckoutOrderRequest checkoutReq = CheckoutOrderRequest.builder()
                .newShippingAddress(shippingAddress)
                .paymentMethodToken("pm_card_visa_saga")
                .couponCode("SAGA20")
                .customerNotes("Please leave by the front door.")
                .build();

        String idempotencyKey = "idemp_saga_" + UUID.randomUUID();

        MvcResult checkoutResult = mockMvc.perform(post("/orders/checkout")
                        .header("Authorization", "Bearer " + customerJwt)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.total.amount").isNotEmpty())
                .andReturn();

        OrderConfirmationResponse confirmation = objectMapper.readValue(
                checkoutResult.getResponse().getContentAsString(), OrderConfirmationResponse.class);
        String orderNumber = confirmation.getOrderNumber();
        assertThat(orderNumber).startsWith("ORD-");

        // =========================================================================
        // STEP 4: Verify Database Invariants (Post-Checkout Saga Verification)
        // =========================================================================

        // 4a. Inventory Decrement Verification (10 - 2 = 8)
        BookFormatEntity formatInDb = bookFormatRepository.findById(format.getId()).orElseThrow();
        assertThat(formatInDb.getInventoryQuantity()).isEqualTo(8);

        // 4b. Cart Cleared Verification
        CartEntity cartInDb = cartRepository.findByUserId(customer.getId()).orElseThrow();
        assertThat(cartInDb.getItems()).isEmpty();

        // 4c. Order Aggregate & Line Items Snapshot Verification
        OrderEntity orderInDb = orderRepository.findByOrderNumberWithDetails(orderNumber).orElseThrow();
        assertThat(orderInDb.getOrderStatus()).isEqualTo("CONFIRMED");
        assertThat(orderInDb.getConfirmedAt()).isNotNull();
        assertThat(orderInDb.getLineItems()).hasSize(1);

        var lineItem = orderInDb.getLineItems().get(0);
        assertThat(lineItem.getQuantity()).isEqualTo(2);
        assertThat(lineItem.getUnitPriceAmount()).isEqualTo(4500L);
        assertThat(lineItem.getBookTitleSnapshot()).isEqualTo("Designing Data-Intensive Applications");
        assertThat(lineItem.getIsbn13Snapshot()).isEqualTo("9781449373320");
        assertThat(lineItem.getFormatTypeSnapshot()).isEqualTo("PAPERBACK");

        // Address snapshots serialized into JSONB
        assertThat(orderInDb.getShippingAddressSnapshot()).contains("10 Downing Street");
        assertThat(orderInDb.getShippingAddressSnapshot()).contains("SW1A 2AA");

        // 4d. Payment Transaction Integrity
        List<PaymentTransactionEntity> paymentTransactions = paymentTransactionRepository.findByOrderId(orderInDb.getId());
        assertThat(paymentTransactions).hasSize(1);
        PaymentTransactionEntity paymentTx = paymentTransactions.get(0);
        assertThat(paymentTx.getTransactionStatus()).isEqualTo("CAPTURED");
        assertThat(paymentTx.getGatewayProvider()).isEqualTo("STRIPE");
        assertThat(paymentTx.getTotalPayableAmount()).isEqualTo(orderInDb.getTotalAmount());
        assertThat(paymentTx.getTenderSplits()).hasSize(1);
        assertThat(paymentTx.getTenderSplits().get(0).getTenderType()).isEqualTo("CREDIT_CARD");

        // 4e. Coupon Redemption Count Increment
        CouponEntity couponInDb = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(couponInDb.getCurrentRedemptions()).isEqualTo(1);

        // 4f. Shipping Consignment Dispatched
        List<ShippingConsignmentEntity> consignments = shippingConsignmentRepository.findByOrderId(orderInDb.getId());
        assertThat(consignments).hasSize(1);
        assertThat(consignments.get(0).getCarrierCode()).isEqualTo("FEDEX");
        assertThat(consignments.get(0).getConsignmentStatus()).isEqualTo("LABEL_CREATED");

        // =========================================================================
        // STEP 5: Query Order Details & Enforce Security Isolation
        // =========================================================================

        // 5a. Customer who placed the order can view details
        MvcResult detailResult = mockMvc.perform(get("/orders/{orderNumber}", orderNumber)
                        .header("Authorization", "Bearer " + customerJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.lineItems[0].bookTitle").value("Designing Data-Intensive Applications"))
                .andReturn();

        OrderDetailResponse orderDetail = objectMapper.readValue(
                detailResult.getResponse().getContentAsString(), OrderDetailResponse.class);
        assertThat(orderDetail.getLineItems()).hasSize(1);

        // 5b. Another customer cannot view this customer's order (Forbidden / BusinessRuleViolation)
        mockMvc.perform(get("/orders/{orderNumber}", orderNumber)
                        .header("Authorization", "Bearer " + otherCustomerJwt)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());

        // =========================================================================
        // STEP 6: Self-Service Order Cancellation & Compensating Inventory Restock
        // =========================================================================
        CancelOrderRequest cancelReq = CancelOrderRequest.builder()
                .reason("Accidentally ordered double copies")
                .build();

        MvcResult cancelResult = mockMvc.perform(post("/orders/{orderNumber}/cancel", orderNumber)
                        .header("Authorization", "Bearer " + customerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(orderNumber))
                .andExpect(jsonPath("$.cancellationStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.refundInitiated").value(true))
                .andReturn();

        OrderCancellationResponse cancellation = objectMapper.readValue(
                cancelResult.getResponse().getContentAsString(), OrderCancellationResponse.class);
        assertThat(cancellation.getCancellationStatus()).isEqualTo("CANCELLED");

        // Post-cancellation Database Verification:
        // 6a. Order marked CANCELLED
        OrderEntity cancelledOrderInDb = orderRepository.findByOrderNumberWithDetails(orderNumber).orElseThrow();
        assertThat(cancelledOrderInDb.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(cancelledOrderInDb.getCancelledAt()).isNotNull();
        assertThat(cancelledOrderInDb.getCancellationReason()).isEqualTo("Accidentally ordered double copies");

        // 6b. Inventory Restock Compensation: 8 restocked back to 10!
        BookFormatEntity restockedFormat = bookFormatRepository.findById(format.getId()).orElseThrow();
        assertThat(restockedFormat.getInventoryQuantity()).isEqualTo(10);

        // 6c. Refund Audit Record
        List<RefundRecordEntity> refunds = refundRecordRepository.findByOrderId(orderInDb.getId());
        assertThat(refunds).hasSize(1);
        RefundRecordEntity refund = refunds.get(0);
        assertThat(refund.getRefundStatus()).isEqualTo("COMPLETED");
        assertThat(refund.getRefundAmount()).isEqualTo(orderInDb.getTotalAmount());
        assertThat(refund.getRefundReason()).isEqualTo("Accidentally ordered double copies");
        assertThat(refund.getGatewayRefundId()).startsWith("re_");
    }
}
