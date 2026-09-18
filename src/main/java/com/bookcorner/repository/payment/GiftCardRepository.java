package com.bookcorner.repository.payment;

import com.bookcorner.entity.payment.GiftCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for GiftCardEntity.
 */
@Repository
public interface GiftCardRepository extends JpaRepository<GiftCardEntity, UUID> {

    /**
     * Find active gift card by card number.
     */
    Optional<GiftCardEntity> findByCardNumberAndIsActiveTrue(String cardNumber);
}
