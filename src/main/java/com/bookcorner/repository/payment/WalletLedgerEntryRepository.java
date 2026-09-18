package com.bookcorner.repository.payment;

import com.bookcorner.entity.payment.WalletLedgerEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for WalletLedgerEntryEntity (append-only ledger).
 */
@Repository
public interface WalletLedgerEntryRepository extends JpaRepository<WalletLedgerEntryEntity, UUID> {

    /**
     * Retrieve paginated ledger entries for a wallet ordered by timestamp descending.
     */
    Page<WalletLedgerEntryEntity> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
}
