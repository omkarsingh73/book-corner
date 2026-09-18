package com.bookcorner.repository.catalog;

import com.bookcorner.entity.catalog.BookFormatEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for BookFormatEntity (format SKUs & inventory balances).
 */
@Repository
public interface BookFormatRepository extends JpaRepository<BookFormatEntity, UUID> {

    /**
     * Find format SKU by unique stock keeping unit code.
     */
    Optional<BookFormatEntity> findBySku(String sku);

    /**
     * Check if a format SKU exists.
     */
    boolean existsBySku(String sku);

    /**
     * Find all available formats for a given book.
     */
    List<BookFormatEntity> findByBookId(UUID bookId);

    /**
     * Find format with pessimistic write lock for atomic inventory reservation during checkout.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bf FROM BookFormatEntity bf WHERE bf.id = :id")
    Optional<BookFormatEntity> findByIdWithPessimisticLock(@Param("id") UUID id);

    /**
     * Atomic stock decrement query preventing negative inventory balances.
     * Returns 1 if stock was decremented, 0 if insufficient stock.
     */
    @Modifying
    @Query("""
        UPDATE BookFormatEntity bf 
        SET bf.stockQuantity = bf.stockQuantity - :quantity,
            bf.version = bf.version + 1
        WHERE bf.id = :formatId 
          AND bf.stockQuantity >= :quantity
        """)
    int decrementInventory(@Param("formatId") UUID formatId, @Param("quantity") int quantity);

    /**
     * Restock inventory on order cancellation or RMA return.
     */
    @Modifying
    @Query("""
        UPDATE BookFormatEntity bf 
        SET bf.stockQuantity = bf.stockQuantity + :quantity,
            bf.version = bf.version + 1
        WHERE bf.id = :formatId
        """)
    void incrementInventory(@Param("formatId") UUID formatId, @Param("quantity") int quantity);
}
