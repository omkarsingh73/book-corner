package com.bookcorner.repository;

import com.bookcorner.config.AbstractPostgresRepositoryTest;
import com.bookcorner.entity.member.GuestSessionEntity;
import com.bookcorner.entity.member.UserEntity;
import com.bookcorner.entity.ordering.CartEntity;
import com.bookcorner.entity.store.StoreEntity;
import com.bookcorner.repository.member.GuestSessionRepository;
import com.bookcorner.repository.member.UserRepository;
import com.bookcorner.repository.ordering.CartRepository;
import com.bookcorner.repository.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CartRepository Integration Tests (PostgreSQL Testcontainers)")
class CartRepositoryTest extends AbstractPostgresRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GuestSessionRepository guestSessionRepository;

    @Autowired
    private StoreRepository storeRepository;

    private UserEntity user;
    private GuestSessionEntity guestSession;
    private StoreEntity store;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        guestSessionRepository.deleteAll();
        userRepository.deleteAll();
        storeRepository.deleteAll();

        user = userRepository.save(UserEntity.builder()
                .email("cart.user@example.com")
                .passwordHash("hashedPass")
                .firstName("Charlie")
                .lastName("Brown")
                .accountStatus("ACTIVE")
                .build());

        store = storeRepository.save(StoreEntity.builder()
                .storeCode("BK_MAIN_ONLINE")
                .storeName("Book Corner Online")
                .defaultCurrency("USD")
                .build());

        guestSession = guestSessionRepository.save(GuestSessionEntity.builder()
                .sessionToken("gst_test_session_token_12345")
                .storeId(store.getId())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .build());
    }

    @Test
    @DisplayName("Should find active cart by registered user ID")
    void shouldFindCartByUserId() {
        CartEntity cart = cartRepository.save(CartEntity.builder()
                .user(user)
                .storeId(store.getId())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .items(new ArrayList<>())
                .build());

        Optional<CartEntity> found = cartRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(cart.getId());
        assertThat(found.get().getUser().getEmail()).isEqualTo("cart.user@example.com");
    }

    @Test
    @DisplayName("Should find active cart by guest session token")
    void shouldFindCartByGuestSessionToken() {
        CartEntity guestCart = cartRepository.save(CartEntity.builder()
                .guestSession(guestSession)
                .storeId(store.getId())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .items(new ArrayList<>())
                .build());

        Optional<CartEntity> found = cartRepository.findByGuestSessionToken("gst_test_session_token_12345");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(guestCart.getId());
        assertThat(found.get().getGuestSession().getSessionToken()).isEqualTo("gst_test_session_token_12345");
    }

    @Test
    @DisplayName("Should check if user has active cart")
    void shouldCheckExistsByUserId() {
        boolean before = cartRepository.existsByUserId(user.getId());
        assertThat(before).isFalse();

        cartRepository.save(CartEntity.builder()
                .user(user)
                .storeId(store.getId())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .items(new ArrayList<>())
                .build());

        boolean after = cartRepository.existsByUserId(user.getId());
        assertThat(after).isTrue();
    }

    @Test
    @DisplayName("Should delete expired carts based on cutoff instant")
    void shouldDeleteExpiredCarts() {
        cartRepository.save(CartEntity.builder()
                .user(user)
                .storeId(store.getId())
                .expiresAt(Instant.now().minus(2, ChronoUnit.DAYS)) // Expired
                .items(new ArrayList<>())
                .build());

        int deletedCount = cartRepository.deleteExpiredCarts(Instant.now());
        assertThat(deletedCount).isEqualTo(1);
        assertThat(cartRepository.findByUserId(user.getId())).isEmpty();
    }
}
