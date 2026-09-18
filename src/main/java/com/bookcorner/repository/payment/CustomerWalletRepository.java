package com.bookcorner.repository.payment;

import com.bookcorner.entity.payment.CustomerWalletEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for CustomerWalletEntity.
 */
@Repository
public interface CustomerWalletRepository extends JpaRepository<CustomerWalletEntity, UUID> {

    /**
     * Find customer wallet by user ID.
     */
    Optional<CustomerWalletEntity> findByUserId(UUID userId);

    /**
     * Retrieve customer wallet with pessimistic write lock for atomic balance debits during checkout.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cw FROM CustomerWalletEntity cw WHERE cw.user.id = :userId")
    Optional<CustomerWalletEntity> findByUserIdWithPessimisticLock(@Param("userId") UUID userId);

    /**
     * Check if a wallet exists for the user.
     */
    boolean existsByUserId(UUID userId);
}
