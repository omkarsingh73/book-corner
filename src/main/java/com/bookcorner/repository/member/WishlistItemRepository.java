package com.bookcorner.repository.member;

import com.bookcorner.entity.member.WishlistItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for WishlistItemEntity.
 */
@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItemEntity, UUID> {

    /**
     * Check if a book is already in a customer's wishlist.
     */
    boolean existsByWishlistIdAndBookId(UUID wishlistId, UUID bookId);

    /**
     * Retrieve paginated items from a wishlist.
     */
    Page<WishlistItemEntity> findByWishlistId(UUID wishlistId, Pageable pageable);

    /**
     * Find single item in a wishlist by book ID.
     */
    Optional<WishlistItemEntity> findByWishlistIdAndBookId(UUID wishlistId, UUID bookId);

    /**
     * Delete an item from a wishlist by book ID.
     */
    @Modifying
    @Query("DELETE FROM WishlistItemEntity wi WHERE wi.wishlist.id = :wishlistId AND wi.book.id = :bookId")
    int deleteByWishlistIdAndBookId(@Param("wishlistId") UUID wishlistId, @Param("bookId") UUID bookId);
}
