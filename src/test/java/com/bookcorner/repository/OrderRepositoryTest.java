package com.bookcorner.repository;

import com.bookcorner.config.AbstractPostgresRepositoryTest;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.ordering.OrderEntity;
import com.bookcorner.entity.store.StoreEntity;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.ordering.OrderRepository;
import com.bookcorner.repository.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderRepository Integration Tests (PostgreSQL Testcontainers)")
class OrderRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoreRepository storeRepository;

    private UserEntity customer;
    private StoreEntity store;
    private OrderEntity order;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
        storeRepository.deleteAll();

        customer = userRepository.save(UserEntity.builder()
                .email("shopper@example.com")
                .passwordHash("hashedPass")
                .firstName("Bob")
                .lastName("Builder")
                .accountStatus("ACTIVE")
                .build());

        store = storeRepository.save(StoreEntity.builder()
                .storeCode("BK_MAIN_ONLINE")
                .storeName("Book Corner Online")
                .defaultCurrency("USD")
                .build());

        order = orderRepository.save(OrderEntity.builder()
                .orderNumber("ORD-20260918-A8F2")
                .user(customer)
                .storeId(store.getId())
                .orderStatus("CONFIRMED")
                .subtotalAmount(4499L)
                .discountAmount(0L)
                .shippingAmount(500L)
                .taxAmount(400L)
                .totalAmount(5399L)
                .currencyCode("USD")
                .shippingAddressSnapshot("{\"recipientName\": \"Bob Builder\", \"city\": \"New York\"}")
                .billingAddressSnapshot("{\"recipientName\": \"Bob Builder\", \"city\": \"New York\"}")
                .placedAt(Instant.now())
                .confirmedAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("Should find order by business order number")
    void shouldFindByOrderNumber() {
        Optional<OrderEntity> found = orderRepository.findByOrderNumber("ORD-20260918-A8F2");

        assertThat(found).isPresent();
        assertThat(found.get().getOrderNumber()).isEqualTo("ORD-20260918-A8F2");
        assertThat(found.get().getTotalAmount()).isEqualTo(5399L);
        assertThat(found.get().getUser().getId()).isEqualTo(customer.getId());
    }

    @Test
    @DisplayName("Should enforce user ownership on order lookup")
    void shouldFindByOrderNumberAndUserId() {
        Optional<OrderEntity> found = orderRepository.findByOrderNumberAndUserId("ORD-20260918-A8F2", customer.getId());
        assertThat(found).isPresent();

        UserEntity otherCustomer = userRepository.save(UserEntity.builder()
                .email("other@example.com")
                .passwordHash("hashedPass")
                .firstName("Eve")
                .lastName("Hacker")
                .accountStatus("ACTIVE")
                .build());

        Optional<OrderEntity> notFound = orderRepository.findByOrderNumberAndUserId("ORD-20260918-A8F2", otherCustomer.getId());
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should paginate customer orders sorted by creation date descending")
    void shouldPaginateCustomerOrders() {
        Page<OrderEntity> history = orderRepository.findByUserIdOrderByCreatedAtDesc(customer.getId(), PageRequest.of(0, 10));

        assertThat(history).isNotEmpty();
        assertThat(history.getTotalElements()).isEqualTo(1);
        assertThat(history.getContent().get(0).getOrderNumber()).isEqualTo("ORD-20260918-A8F2");
    }

    @Test
    @DisplayName("Should filter customer orders by status")
    void shouldFilterCustomerOrdersByStatus() {
        Page<OrderEntity> confirmed = orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(
                customer.getId(), "CONFIRMED", PageRequest.of(0, 10));
        Page<OrderEntity> delivered = orderRepository.findByUserIdAndOrderStatusOrderByCreatedAtDesc(
                customer.getId(), "DELIVERED", PageRequest.of(0, 10));

        assertThat(confirmed).isNotEmpty();
        assertThat(delivered).isEmpty();
    }

    @Test
    @DisplayName("Should count total orders placed by customer")
    void shouldCountCustomerOrders() {
        long count = orderRepository.countByUserId(customer.getId());
        assertThat(count).isEqualTo(1);
    }
}
