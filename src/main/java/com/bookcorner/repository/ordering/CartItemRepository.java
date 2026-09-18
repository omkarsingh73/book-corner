package com.bookcorner.repository.ordering;

import com.bookcorner.entity.ordering.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for CartItemEntity.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {

    /**
     * Find item in cart for a specific format SKU.
     */
    Optional<CartItemEntity> findByCartIdAndFormatId(UUID cartId, UUID formatId);

    /**
     * Retrieve all items in a cart.
     */
    List<CartItemEntity> findByCartId(UUID cartId);

    /**
     * Check if a format SKU is present in the cart.
     */
    boolean existsByCartIdAndFormatId(UUID cartId, UUID formatId);

    /**
     * Remove an item from a cart by format ID.
     */
    @Modifying
    @Query("DELETE FROM CartItemEntity ci WHERE ci.cart.id = :cartId AND ci.format.id = :formatId")
    int deleteByCartIdAndFormatId(@Param("cartId") UUID cartId, @Param("formatId") UUID formatId);

    /**
     * Clear all items from a cart.
     */
    @Modifying
    @Query("DELETE FROM CartItemEntity ci WHERE ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") UUID cartId);
}
