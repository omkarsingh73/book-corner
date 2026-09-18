package com.bookcorner.repository.member;

import com.bookcorner.entity.member.WishlistEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for WishlistEntity.
 */
@Repository
public interface WishlistRepository extends JpaRepository<WishlistEntity, UUID> {

    /**
     * Find customer wishlist by user ID with eager loading of book formats and authors.
     */
    @EntityGraph(attributePaths = {"items.book.formats", "items.book.bookAuthors.author"})
    Optional<WishlistEntity> findByUserId(UUID userId);

    /**
     * Check if a wishlist already exists for the user.
     */
    boolean existsByUserId(UUID userId);
}
